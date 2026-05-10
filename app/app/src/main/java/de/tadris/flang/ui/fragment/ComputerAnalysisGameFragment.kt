package de.tadris.flang.ui.fragment

import android.app.AlertDialog
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.annotation.WorkerThread
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.lifecycleScope
import androidx.viewpager2.widget.ViewPager2
import de.tadris.flang.R
import de.tadris.flang.bot.NativeCFlangEngine
import de.tadris.flang.databinding.ViewComputerAnalysisBinding
import de.tadris.flang.network.DataRepository
import de.tadris.flang.network_api.exception.TooManyRequestsException
import de.tadris.flang.network_api.util.AnalysisSerializer
import de.tadris.flang.ui.board.ArrowFieldView
import de.tadris.flang.ui.board.JudgmentAnnotationView
import de.tadris.flang.ui.fragment.analysis.AnalysisPagerAdapter
import de.tadris.flang.ui.fragment.analysis.AnalysisViewModel
import de.tadris.flang_lib.Game
import de.tadris.flang_lib.analysis.AnalysisListener
import de.tadris.flang_lib.analysis.AnalysisResult
import de.tadris.flang_lib.analysis.ComputerAnalysis
import de.tadris.flang_lib.analysis.MoveInfo
import de.tadris.flang_lib.analysis.MoveJudgmentType
import de.tadris.flang_lib.Board
import de.tadris.flang_lib.getToIndex
import de.tadris.flang_lib.isResign
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class ComputerAnalysisGameFragment : AbstractAnalysisGameFragment(), AnalysisListener {

    companion object {
        private const val OFFLINE_ANALYSIS_DEPTH = 5
    }

    private var _analysisBinding: ViewComputerAnalysisBinding? = null
    private val analysisBinding get() = _analysisBinding!!
    
    private val analysisViewModel: AnalysisViewModel by activityViewModels()

    private var state = AnalysisUIState.NO_ANALYSIS_AVAILABLE
    private var analysisResult: AnalysisResult? = null
    private var serverAnalysisId: Long? = null
    private var isUsingServerAnalysis = false
    
    private lateinit var pagerAdapter: AnalysisPagerAdapter
    private var currentMoveIndex = 0
    private var isUpdatingMoveIndex = false

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        val view = super.onCreateView(inflater, container, savedInstanceState)

        analysisViewModel.clearData()

        binding.gameAdditionalContentParent.visibility = View.VISIBLE
        binding.hintButton.visibility = View.GONE
        binding.positionName.visibility = View.GONE

        _analysisBinding = ViewComputerAnalysisBinding.inflate(inflater, binding.gameAdditionalContentParent, true)

        setupPager()
        setupTabs()

        analysisBinding.requestAnalysisButton.setOnClickListener {
            checkQuotaAndBeginAnalysis()
        }

        boardView.listener = null // disable user interaction

        if(state == AnalysisUIState.NO_ANALYSIS_AVAILABLE){
            checkQuotaAndBeginAnalysis() // automatic analysis, can be removed if there is a selection
        }

        return view
    }

    override fun onResume() {
        super.onResume()
        refreshUI()
    }

    override fun onDestroyView() {
        // Clean up board annotations when leaving the fragment
        boardView.detachAllArrows()
        boardView.detachAllAnnotations()
        _analysisBinding = null
        super.onDestroyView()
    }

    private fun setupPager() {
        pagerAdapter = AnalysisPagerAdapter(this)
        analysisBinding.analysisViewPager.adapter = pagerAdapter

        setupViewModelObservers()
        
        analysisBinding.analysisViewPager.registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
            override fun onPageSelected(position: Int) {
                updateTabSelection(position)
            }
        })
    }
    
    private fun setupViewModelObservers() {
        analysisViewModel.currentMoveIndex.observe(viewLifecycleOwner) { moveIndex ->
            Log.d("ComputerAnalysisGameFragment", "ViewModel moveIndex changed to: $moveIndex, current: $currentMoveIndex, isUpdating: $isUpdatingMoveIndex")
            if(currentMoveIndex == moveIndex || isUpdatingMoveIndex) return@observe
            
            isUpdatingMoveIndex = true
            currentMoveIndex = moveIndex
            navigateToMove(moveIndex)
            isUpdatingMoveIndex = false
        }
    }
    
    private fun setupTabs() {
        analysisBinding.tabOverview.setOnClickListener { 
            analysisBinding.analysisViewPager.currentItem = 0 
        }
        analysisBinding.tabChart.setOnClickListener { 
            analysisBinding.analysisViewPager.currentItem = 1 
        }
        analysisBinding.tabMoves.setOnClickListener { 
            analysisBinding.analysisViewPager.currentItem = 2 
        }
        
        updateTabSelection(0)
    }
    
    private fun updateTabSelection(position: Int) {
        val primaryColor = requireContext().getColor(R.color.colorPrimary)
        val secondaryColor = requireContext().getColor(android.R.color.darker_gray)
        
        analysisBinding.tabOverview.setTextColor(if (position == 0) primaryColor else secondaryColor)
        analysisBinding.tabChart.setTextColor(if (position == 1) primaryColor else secondaryColor)
        analysisBinding.tabMoves.setTextColor(if (position == 2) primaryColor else secondaryColor)
        
        // When switching to moves tab, update the current selection
        if (position == 2) {
            analysisViewModel.setCurrentMoveIndex(currentMoveIndex)
        }
    }

    private fun refreshUI(){
        when(state){
            AnalysisUIState.NO_ANALYSIS_AVAILABLE -> {
                analysisBinding.analysisContentParent.visibility = View.GONE
                analysisBinding.requestAnalysisButton.visibility = View.VISIBLE
                analysisBinding.analyzingParent.visibility = View.GONE
            }
            AnalysisUIState.CHECKING_QUOTA -> {
                analysisBinding.analyzingParent.visibility = View.VISIBLE
                analysisBinding.analysisContentParent.alpha = 0.3f
                analysisBinding.requestAnalysisButton.visibility = View.GONE
                updateAnalyzingText(getString(R.string.checkingQuota))
            }
            AnalysisUIState.REQUESTING_ANALYSIS -> {
                analysisBinding.analyzingParent.visibility = View.VISIBLE
                analysisBinding.analysisContentParent.alpha = 0.3f
                analysisBinding.requestAnalysisButton.visibility = View.GONE
                updateAnalyzingText(getString(R.string.requestingAnalysis))
            }
            AnalysisUIState.ANALYZING -> {
                analysisBinding.analyzingParent.visibility = View.VISIBLE
                analysisBinding.analysisContentParent.alpha = 0.3f
                analysisBinding.requestAnalysisButton.visibility = View.GONE
                val text = if (isUsingServerAnalysis) getString(R.string.analysisRequested) else getString(R.string.analyzingGame)
                updateAnalyzingText(text)
            }
            AnalysisUIState.SHOW_ANALYSIS -> {
                analysisBinding.requestAnalysisButton.visibility = View.GONE
                analysisBinding.analysisContentParent.alpha = 1f
                analysisBinding.analyzingParent.visibility = View.GONE
            }
        }
        if(analysisResult != null){
            analysisBinding.analysisContentParent.visibility = View.VISIBLE
            updateAnalysisData()
            onDisplayedBoardChange(displayedBoard.currentState)
        }
    }
    
    private fun updateAnalyzingText(text: String) {
        val textView = analysisBinding.root.findViewById<android.widget.TextView>(R.id.analyzingText)
        textView?.text = text
    }
    
    private fun updateAnalysisData() {
        val result = analysisResult ?: return
        
        Log.d("ComputerAnalysisGameFragment", "Setting analysis result with ${result.moves.size} moves")
        
        // Update the ViewModel which will notify all fragments
        analysisViewModel.setAnalysisResult(result)
        
        // Also set the current move index to ensure fragments are in sync
        Log.d("ComputerAnalysisGameFragment", "Setting current move index: $currentMoveIndex")
        analysisViewModel.setCurrentMoveIndex(currentMoveIndex)
    }

    private fun checkQuotaAndBeginAnalysis() {
        viewLifecycleOwner.lifecycleScope.launch {
            val dataRepository = DataRepository.getInstance()
            if (dataRepository.credentialsAvailable(requireContext())) {
                beginServerAnalysis()
            } else {
                // User not logged in, use offline analysis
                beginOfflineAnalysis()
            }
        }
    }
    
    private fun showQuotaExceededDialog() {
        viewLifecycleOwner.lifecycleScope.launch {
            val dialogView = layoutInflater.inflate(R.layout.dialog_quota_limit, null)
            
            // Check and display current quota information
            try {
                val quota = withContext(Dispatchers.IO) {
                    val dataRepository = DataRepository.getInstance()
                    dataRepository.accessRestrictedAPI(requireContext()).getAnalysisQuota()
                }
                
                // Update dialog message with quota information
                val quotaMessage = getString(R.string.analysisQuotaExceededMessage) + 
                    "\n\n" + getString(R.string.analysisQuotaInfo, quota.used, quota.dailyLimit)
                dialogView.findViewById<android.widget.TextView>(R.id.quotaMessage).text = quotaMessage
                
            } catch (e: Exception) {
                // If quota check fails, use default message
            }
            
            val alertDialog = AlertDialog.Builder(requireContext())
                .setView(dialogView)
                .setCancelable(false)
                .create()
            
            dialogView.findViewById<View>(R.id.btnClose).setOnClickListener {
                alertDialog.dismiss()
                state = AnalysisUIState.NO_ANALYSIS_AVAILABLE
                refreshUI()
            }
            
            dialogView.findViewById<View>(R.id.btnUseOffline).setOnClickListener {
                alertDialog.dismiss()
                beginOfflineAnalysis()
            }
            
            alertDialog.show()
        }
    }
    
    private fun beginServerAnalysis() {
        viewLifecycleOwner.lifecycleScope.launch {
            state = AnalysisUIState.REQUESTING_ANALYSIS
            refreshUI()
            
            try {
                val dataRepository = DataRepository.getInstance()
                val analysisRequest = withContext(Dispatchers.IO) {
                    dataRepository.accessRestrictedAPI(requireContext()).requestAnalysis(fmn)
                }
                
                serverAnalysisId = analysisRequest.id
                isUsingServerAnalysis = true
                state = AnalysisUIState.ANALYZING
                refreshUI()
                
                // Start polling for results
                pollServerAnalysis()
                
            } catch (e: TooManyRequestsException) {
                // Show quota exceeded dialog with current quota info
                showQuotaExceededDialog()
            } catch (e: Exception) {
                // If server analysis fails, fall back to offline analysis
                beginOfflineAnalysis()
            }
        }
    }
    
    private fun pollServerAnalysis() {
        viewLifecycleOwner.lifecycleScope.launch {
            val analysisId = serverAnalysisId ?: return@launch

            delay(1000)
            while (state == AnalysisUIState.ANALYZING && isUsingServerAnalysis) {
                try {
                    val result = withContext(Dispatchers.IO) {
                        val dataRepository = DataRepository.getInstance()
                        dataRepository.accessRestrictedAPI(requireContext()).getAnalysisResult(analysisId)
                    }
                    
                    if (result.isFinished) {
                        result.data?.let { data ->
                            analysisResult = AnalysisSerializer.deserialize(data)
                            state = AnalysisUIState.SHOW_ANALYSIS
                            refreshUI()
                        }
                        break
                    } else {
                        val progress = (result.progress * 100).toInt()
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                            analysisBinding.analyzingProgress.setProgress(progress, true)
                        } else {
                            analysisBinding.analyzingProgress.progress = progress
                        }
                        updateAnalyzingText(getString(
                            if(progress > 0) R.string.waitingForAnalysis else R.string.analysisQueued
                        ))

                        delay(3000)
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                    // If polling fails, fall back to offline analysis
                    beginOfflineAnalysis()
                    break
                }
            }
        }
    }
    
    private fun beginOfflineAnalysis() {
        isUsingServerAnalysis = false
        viewLifecycleOwner.lifecycleScope.launch {
            state = AnalysisUIState.ANALYZING
            refreshUI()
            delay(500)
            analyze(OFFLINE_ANALYSIS_DEPTH)
            state = AnalysisUIState.SHOW_ANALYSIS
            refreshUI()
        }
    }
    
    private fun navigateToMove(moveIndex: Int) {
        Log.d("ComputerAnalysisGameFragment", "navigateToMove called with index: $moveIndex")
        val actions = gameBoard.moveList
        if (moveIndex < actions.size) {
            val subActions = actions.subList(0, moveIndex + 1).toMutableList()
            Log.d("ComputerAnalysisGameFragment", "Setting displayedBoard and refreshing")
            displayedBoard = Game.fromActionList(subActions)
            refreshBoardView()
            showMoveAnalysis(moveIndex)
        } else {
            Log.w("ComputerAnalysisGameFragment", "moveIndex $moveIndex >= actions.size ${actions.size}")
        }
    }
    
    private fun showMoveAnalysis(moveIndex: Int) {
        val result = analysisResult ?: return
        val moves = result.moves
        
        if (moveIndex < moves.size) {
            val moveInfo = moves[moveIndex]
            showMoveOnBoard(moveInfo)
        }
    }
    
    private fun showMoveOnBoard(moveInfo: MoveInfo) {
        // Clear all previous annotations and arrows
        boardView.detachAllArrows()
        boardView.detachAllAnnotations()
        attachDefaultFieldAnnotations()

        val analysisResult = analysisResult ?: return
        val action = moveInfo.getAction(analysisResult.fmn)
        if (!action.isResign()) {
            // Show the move arrow
            val color = getJudgmentColor(moveInfo.judgment?.type)
            boardView.attach(ArrowFieldView(context, action, boardView, color))
            
            // Show judgment annotation on the destination square
            moveInfo.judgment?.let { judgment ->
                val destinationLocation = action.getToIndex()
                boardView.attach(JudgmentAnnotationView(requireContext(), destinationLocation, judgment.type))
            }
            
            // Show best move if different
            val bestMove = moveInfo.getBestMove(analysisResult.fmn)
            if (bestMove != null && moveInfo.judgment?.isError == true) {
                val bestMoveColor = requireContext().resources.getColor(R.color.green_700_faded, null)
                boardView.attach(ArrowFieldView(context, bestMove, boardView, bestMoveColor))
            }
        }
    }
    
    private fun getJudgmentColor(judgmentType: MoveJudgmentType?): Int {
        return when (judgmentType) {
            MoveJudgmentType.EXCELLENT, MoveJudgmentType.GOOD -> 
                requireContext().resources.getColor(R.color.green_700, null)
            MoveJudgmentType.INACCURACY -> 
                requireContext().resources.getColor(R.color.yellow_700, null)
            MoveJudgmentType.MISTAKE, MoveJudgmentType.BLUNDER -> 
                requireContext().resources.getColor(R.color.red_700, null)
            else -> 
                requireContext().resources.getColor(R.color.colorPrimary, null)
        }
    }

    override fun onDisplayedBoardChange(board: Board) {
        val moveIndex = board.moveNumber - 1
        if (moveIndex >= 0) {
            currentMoveIndex = moveIndex
            analysisViewModel.setCurrentMoveIndex(moveIndex)
        }
        showCurrentMoveAnalysis()
    }
    
    private fun showCurrentMoveAnalysis() {
        val result = analysisResult ?: return
        if (currentMoveIndex >= 0 && currentMoveIndex < result.moves.size) {
            val moveInfo = result.moves[currentMoveIndex]
            showMoveOnBoard(moveInfo)
        } else {
            // Clear annotations if no valid move
            boardView.detachAllArrows()
            boardView.detachAllAnnotations()
            attachDefaultFieldAnnotations()
        }
    }

    override fun isGameClickable() = true

    override fun getNavigationLinkToAnalysis() = R.id.action_nav_computer_analysis_to_nav_analysis
    override fun getNavigationLinkToChat() = R.id.action_nav_computer_analysis_to_nav_chat

    @WorkerThread
    private suspend fun analyze(depth: Int) = withContext(Dispatchers.IO){
        val engine = NativeCFlangEngine(1, depth)
        analysisResult = ComputerAnalysis(fmn, engine, this@ComputerAnalysisGameFragment).analyze()
        engine.destroy()
    }

    enum class AnalysisUIState {
        NO_ANALYSIS_AVAILABLE,
        CHECKING_QUOTA,
        REQUESTING_ANALYSIS,
        ANALYZING,
        SHOW_ANALYSIS
    }

    // AnalysisListener implementation
    override fun onAnalysisStarted(totalMoves: Int) {
        // Could show more detailed progress if needed
    }
    
    override fun onMoveAnalyzed(currentMove: Int, totalMoves: Int, moveInfo: MoveInfo?) {
        val progress = (currentMove * 100) / totalMoves
        if(_analysisBinding == null) return
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            analysisBinding.analyzingProgress.setProgress(progress, true)
        } else {
            analysisBinding.analyzingProgress.progress = progress
        }
    }
    
    override fun onAnalysisCompleted(result: AnalysisResult) {
        // Analysis is complete - UI will be updated in the main thread
    }

}