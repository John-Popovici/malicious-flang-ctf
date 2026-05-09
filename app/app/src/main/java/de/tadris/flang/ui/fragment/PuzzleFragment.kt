package de.tadris.flang.ui.fragment

import android.os.Build
import android.os.Bundle
import android.view.HapticFeedbackConstants
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.button.MaterialButton
import kotlinx.coroutines.launch
import de.tadris.flang.R
import de.tadris.flang.game.GameController
import de.tadris.flang.game.PuzzleGameController
import de.tadris.flang.network_api.model.GameInfo
import de.tadris.flang_lib.Game
import de.tadris.flang_lib.Color
import java.util.Locale
import kotlin.math.abs

class PuzzleFragment : GameFragment(), PuzzleGameController.PuzzleCallback {

    private lateinit var puzzleGameController: PuzzleGameController
    private var puzzleResultBottomSheet: BottomSheetDialog? = null
    private var noPuzzlesBottomSheet: BottomSheetDialog? = null

    init {
        setHasOptionsMenu(true)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        puzzleGameController = PuzzleGameController(requireActivity(), this)
        super.onCreate(savedInstanceState)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupPuzzleUI()
    }

    override fun createGameController(): GameController {
        return puzzleGameController
    }

    override fun getNavigationLinkToAnalysis(): Int {
        return R.id.action_nav_puzzles_to_nav_analysis
    }

    override fun getNavigationLinkToChat(): Int {
        return R.id.action_nav_puzzles_to_nav_chat
    }

    private fun setupPuzzleUI() {
        
        // Hide some buttons not relevant for puzzles
        binding.swapSidesButton.visibility = View.GONE
        binding.shareButton.visibility = View.GONE
        binding.resignButton.visibility = View.GONE
        
        // Repurpose hint button as "Next Puzzle" button
        binding.hintButton.setImageResource(R.drawable.ic_skip)
        binding.hintButton.contentDescription = getString(R.string.nextPuzzle)
        binding.hintButton.setOnClickListener {
            continueToNextPuzzle()
        }
        updateNextPuzzleButton()
    }

    override fun onGameRequestSuccess(
        info: GameInfo,
        isParticipant: Boolean,
        color: Color?,
        board: Game?
    ) {
        super.onGameRequestSuccess(info, isParticipant, color, board)
        binding.resignButton.visibility = View.GONE

        updateNextPuzzleButton()
    }

    override fun onUpdate(gameInfo: GameInfo) {
        super.onUpdate(gameInfo)
        
        // Show/hide the next puzzle button based on puzzle completion
        updateNextPuzzleButton()
        
        if (puzzleGameController.isPuzzleSolved()) {
            showPuzzleSolvedFeedback()
        }
    }
    
    private fun updateNextPuzzleButton() {
        // Show the hint button (repurposed as next puzzle) when puzzle is solved
        binding.hintButton.visibility = if (puzzleGameController.isPuzzleSolved()) {
            View.VISIBLE
        } else {
            View.GONE
        }
    }

    private fun showPuzzleSolvedFeedback() {
        showPuzzleResultBottomSheet()
    }

    private fun showPuzzleResultBottomSheet() {
        val bottomSheetView = LayoutInflater.from(requireContext())
            .inflate(R.layout.bottom_sheet_puzzle_result, null)
        
        puzzleResultBottomSheet = BottomSheetDialog(requireContext()).apply {
            setContentView(bottomSheetView)
            setCancelable(true)
        }

        // Setup rating display
        setupRatingDisplay(bottomSheetView)
        
        // Setup vote buttons and related UI - hide in custom puzzle mode
        val ratingQuestion = bottomSheetView.findViewById<TextView>(R.id.ratingQuestion)
        val btnVoteUp = bottomSheetView.findViewById<MaterialButton>(R.id.btnVoteUp)
        val btnVoteDown = bottomSheetView.findViewById<MaterialButton>(R.id.btnVoteDown)
        val btnNextPuzzle = bottomSheetView.findViewById<MaterialButton>(R.id.btnNextPuzzle)

        if (puzzleGameController.isCustomPuzzleMode()) {
            ratingQuestion.visibility = View.GONE
            btnVoteUp.visibility = View.GONE
            btnVoteDown.visibility = View.GONE
            btnNextPuzzle.visibility = View.GONE
        } else {
            btnVoteUp.setOnClickListener {
                puzzleGameController.sendRatePuzzle(1)
                Toast.makeText(context, getString(R.string.thanksForFeedback), Toast.LENGTH_SHORT).show()
                continueToNextPuzzle()
            }

            btnVoteDown.setOnClickListener {
                puzzleGameController.sendRatePuzzle(-1)
                Toast.makeText(context, getString(R.string.thanksForFeedback), Toast.LENGTH_SHORT).show()
                continueToNextPuzzle()
            }

            btnNextPuzzle.setOnClickListener {
                continueToNextPuzzle()
            }
        }
        
        // Setup continue on board button
        bottomSheetView.findViewById<Button>(R.id.btnSkipVoting).setOnClickListener {
            // Just dismiss the bottom sheet, stay on current board
            puzzleResultBottomSheet?.dismiss()
            puzzleResultBottomSheet = null
        }
        
        puzzleResultBottomSheet?.show()
    }
    
    private fun continueToNextPuzzle() {
        puzzleResultBottomSheet?.dismiss()
        puzzleResultBottomSheet = null

        // If we were in custom puzzle mode, reset to normal mode before loading next puzzle
        if (puzzleGameController.isCustomPuzzleMode()) {
            puzzleGameController.resetToNormalMode()
        }

        // Load next puzzle
        puzzleGameController.nextPuzzle()
    }
    
    fun showNoPuzzlesAvailableBottomSheet() {
        val bottomSheetView = LayoutInflater.from(requireContext())
            .inflate(R.layout.bottom_sheet_no_puzzles, null)
        
        noPuzzlesBottomSheet = BottomSheetDialog(requireContext()).apply {
            setContentView(bottomSheetView)
            setCancelable(true)
        }
        
        // Setup close button
        bottomSheetView.findViewById<Button>(R.id.btnClose).setOnClickListener {
            noPuzzlesBottomSheet?.dismiss()
            noPuzzlesBottomSheet = null
        }
        
        noPuzzlesBottomSheet?.show()
    }

    override fun onGameCompleted() { }

    override fun notifyMoveCorrectness(correct: Boolean) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            if (!correct) {
                boardView.rootView.performHapticFeedback(HapticFeedbackConstants.REJECT)
            }
        }
    }

    override fun notifyPuzzleCompleted(correct: Boolean) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            boardView.rootView.performHapticFeedback(HapticFeedbackConstants.CONFIRM)
        }
    }

    override fun onGameRequestFailed(reason: String) {
        // Check if the reason is specifically "no puzzles available"
        if (reason == getString(R.string.noPuzzlesAvailable)) {
            showNoPuzzlesAvailableBottomSheet()
        } else {
            // For other errors, use the default behavior
            super.onGameRequestFailed(reason)
        }
    }

    private fun setupRatingDisplay(bottomSheetView: View) {
        val currentRating = bottomSheetView.findViewById<TextView>(R.id.currentRating)
        val ratingChange = bottomSheetView.findViewById<TextView>(R.id.ratingChange)

        // Get rating change data from controller
        val (oldRating, _, change) = puzzleGameController.getRatingChange()

        // Display current rating (after puzzle completion)
        currentRating.text = String.format(Locale.US, "%.0f", oldRating)

        // Display rating change
        if (abs(change) >= 0.1f) {
            ratingChange.visibility = View.VISIBLE
            val changeText = if (change > 0) {
                "+${String.format(Locale.US, "%.0f", change)}"
            } else {
                String.format(Locale.US, "%.0f", change)
            }
            ratingChange.text = changeText

            // Set color based on rating change
            val colorResId = when {
                change > 0 -> R.color.ratingDiffPositive
                change < 0 -> R.color.ratingDiffNegative
                else -> R.color.ratingDiffNeutral
            }
            ratingChange.setTextColor(ContextCompat.getColor(requireContext(), colorResId))
        } else {
            ratingChange.visibility = View.GONE
        }
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        inflater.inflate(R.menu.fragment_puzzle, menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == R.id.actionEnterPuzzleId) {
            showPuzzleIdInputDialog()
            return true
        }
        return super.onOptionsItemSelected(item)
    }

    private fun showPuzzleIdInputDialog() {
        val editText = EditText(requireContext()).apply {
            hint = getString(R.string.puzzleIdHint)
            inputType = android.text.InputType.TYPE_CLASS_NUMBER
        }

        AlertDialog.Builder(requireActivity())
            .setTitle(R.string.enterPuzzleId)
            .setView(editText)
            .setPositiveButton(R.string.okay) { _, _ ->
                val puzzleIdText = editText.text.toString().trim()
                if (puzzleIdText.isNotEmpty()) {
                    try {
                        val puzzleId = puzzleIdText.toLong()
                        loadPuzzleById(puzzleId)
                    } catch (e: NumberFormatException) {
                        Toast.makeText(requireContext(), getString(R.string.invalidPuzzleId), Toast.LENGTH_SHORT).show()
                    }
                } else {
                    Toast.makeText(requireContext(), getString(R.string.puzzleIdRequired), Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton(R.string.actionCancel, null)
            .show()
    }

    private fun loadPuzzleById(puzzleId: Long) {
        viewLifecycleOwner.lifecycleScope.launch {
            try {
                puzzleGameController.loadPuzzleById(puzzleId)
            } catch (e: Exception) {
                val (title, message) = when {
                    e.message?.contains("404") == true || e is de.tadris.flang.network_api.exception.NotFoundException ->
                        Pair(getString(R.string.puzzleNotFound), getString(R.string.puzzleNotFoundMessage))
                    else ->
                        Pair(getString(R.string.error), getString(R.string.failedToFetchPuzzle, e.message ?: ""))
                }

                AlertDialog.Builder(requireActivity())
                    .setTitle(title)
                    .setMessage(message)
                    .setPositiveButton(R.string.okay, null)
                    .show()
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        puzzleResultBottomSheet?.dismiss()
        noPuzzlesBottomSheet?.dismiss()
    }
}