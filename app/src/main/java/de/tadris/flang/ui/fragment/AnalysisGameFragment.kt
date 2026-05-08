package de.tadris.flang.ui.fragment

import android.os.Bundle
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.Toast
import androidx.annotation.WorkerThread
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import de.tadris.flang.R
import de.tadris.flang.network.DataRepository
import de.tadris.flang.network_api.model.GameInfo
import de.tadris.flang.network_api.model.OpeningDatabaseEntry
import de.tadris.flang.ui.adapter.GameAdapter
import de.tadris.flang.ui.adapter.OpeningDatabaseEntriesAdapter
import de.tadris.flang.ui.board.AnnotationFieldView
import de.tadris.flang.ui.board.FieldView
import de.tadris.flang.ui.dialog.ImportType
import de.tadris.flang.ui.dialog.openImportDialog
import de.tadris.flang_lib.Board
import de.tadris.flang_lib.bot.evaluation.FastNeoBoardEvaluation
import de.tadris.flang_lib.parseMove
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.math.roundToInt

class AnalysisGameFragment : AbstractAnalysisGameFragment(), OpeningDatabaseEntriesAdapter.OpeningDatabaseEntryListener, GameAdapter.GameAdapterListener {

    private var openingDatabaseVisible = false
    private var openingDatabase: RecyclerView? = null
    private var openingDatabaseGames: RecyclerView? = null
    private val openingDatabaseAdapter = OpeningDatabaseEntriesAdapter(this, emptyList())
    private var openingDatabaseGamesAdapter = GameAdapter(mutableListOf(), this)

    init {
        setHasOptionsMenu(true)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        val v = super.onCreateView(inflater, container, savedInstanceState)
        if(running){
            // Hide computer hints for running games
            binding.hintButton.visibility = View.GONE
        }else{
            binding.computerAnalysisButton.visibility = View.VISIBLE
        }
        binding.openingDatabaseToggleButton.visibility = View.VISIBLE
        binding.openingDatabaseToggleButton.setOnClickListener { toggleOpeningDatabaseView() }
        binding.analysisButton.visibility = View.GONE

        if(openingDatabaseVisible){
            addOpeningDatabaseView()
        }

        return v
    }

    override fun onDestroyView() {
        removeOpeningDatabaseView()
        openingDatabase = null
        openingDatabaseGames = null
        super.onDestroyView()
    }

    private fun toggleOpeningDatabaseView(){
        if(openingDatabaseVisible){
            openingDatabaseVisible = false
            removeOpeningDatabaseView()
            Toast.makeText(requireContext(), R.string.openingDatabaseDisabled, Toast.LENGTH_SHORT).show()
        }else{
            openingDatabaseVisible = true
            addOpeningDatabaseView()
            Toast.makeText(requireContext(), R.string.openingDatabaseEnabled, Toast.LENGTH_SHORT).show()
        }
    }

    private fun addOpeningDatabaseView(){
        binding.positionName.visibility = View.GONE
        binding.gameAdditionalContentParent.visibility = View.VISIBLE

        // Create container for both entries and games
        val container = LinearLayout(requireContext()).apply {
            orientation = LinearLayout.VERTICAL
            layoutParams = ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            )
        }

        if(openingDatabase == null){
            openingDatabase = RecyclerView(requireContext())
            openingDatabase!!.layoutManager = LinearLayoutManager(context)
            openingDatabase!!.adapter = openingDatabaseAdapter
        }
        container.addView(openingDatabase)

        if(openingDatabaseGames == null){
            openingDatabaseGames = RecyclerView(requireContext())
            openingDatabaseGames!!.layoutManager = LinearLayoutManager(context)
            openingDatabaseGames!!.adapter = openingDatabaseGamesAdapter
        }
        container.addView(openingDatabaseGames)

        binding.gameAdditionalContentParent.addView(container)
        refreshOpeningDatabase()
    }

    private fun removeOpeningDatabaseView(){
        binding.positionName.visibility = View.VISIBLE
        binding.gameAdditionalContentParent.visibility = View.GONE

        binding.gameAdditionalContentParent.removeAllViews()
    }

    override fun onEntryClick(entry: OpeningDatabaseEntry) {
        try {
            val move = parseMove(displayedBoard.currentState, entry.move)
            onMoveRequested(move)
        }catch (e: Exception){
            e.printStackTrace()
        }
    }

    override fun getPositionName(entry: OpeningDatabaseEntry): String? {
        val copy = displayedBoard.copy()
        copy.execute(parseMove(displayedBoard.currentState, entry.move))
        return positions.findCurrentPositionName(copy)
    }

    override fun onClick(gameInfo: GameInfo) {
        val bundle = Bundle()
        bundle.putLong(OnlineGameFragment.EXTRA_GAME_ID, gameInfo.gameId)
        findNavController().navigate(R.id.action_nav_analysis_to_nav_game, bundle)
    }

    override fun onDisplayedBoardChange(board: Board) {
        if(openingDatabaseVisible){
            refreshOpeningDatabase()
        }

        /*val eval = FastNeoBoardEvaluation()
        eval.evaluate(board)
        val breakdown = eval.evaluateBreakdown()
        boardView.detachAllAnnotations()
        breakdown.evaluationMatrix.forEachIndexed { index, evaluation ->
            if(evaluation != null){
                boardView.attach(AnnotationFieldView(requireContext(), index, evaluation.evaluateField().roundToInt().toString()))
            }
        }
        binding.positionName.text = breakdown.factors.entries.joinToString(separator = "\n") { "${it.key}: ${it.value.roundToInt()}" }*/
    }

    private fun refreshOpeningDatabase(){
        viewLifecycleOwner.lifecycleScope.launch {
            try{
                val result = queryOpeningDatabase()
                openingDatabaseAdapter.updateList(result.result)
                updateGamesAdapter(result.games)
            }catch (e: Exception){
                // TODO show error message
                e.printStackTrace()
            }
        }
    }

    private fun updateGamesAdapter(games: List<GameInfo>) {
        // Recreate the adapter with the new games list to ensure clean state
        openingDatabaseGamesAdapter = GameAdapter(games.toMutableList(), this)
        openingDatabaseGames?.adapter = openingDatabaseGamesAdapter
    }

    @WorkerThread
    private suspend fun queryOpeningDatabase() = withContext(Dispatchers.IO) {
        DataRepository.getInstance().accessOpenAPI().queryOpeningDatabase(displayedBoard.getFMNv1())
    }

    override fun isGameClickable() = true

    override fun getNavigationLinkToAnalysis() = R.id.action_nav_analysis_to_nav_computer_analysis
    override fun getNavigationLinkToChat() = R.id.action_nav_analysis_to_nav_chat

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        inflater.inflate(R.menu.fragment_analysis, menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if(item.itemId == R.id.actionImportGame){
            openImportDialog { gameString, board, type ->
                when(type){
                    ImportType.FBN2 -> {
                        baseBoard = board.initialBoard
                        firstBoard = board
                        gameBoard = board.copy()
                    }
                    ImportType.FMN -> {
                        fmn = gameString
                        firstBoard = board
                        gameBoard = board.copy()
                    }
                }
                setDisplayedBoardToGameBoard(force = true)
            }
            return true
        }
        return super.onOptionsItemSelected(item)
    }
}