package de.tadris.flang.ui.fragment.analysis

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import de.tadris.flang.R
import de.tadris.flang.databinding.FragmentAnalysisOverviewBinding
import de.tadris.flang_lib.analysis.AccuracyCalculator
import de.tadris.flang_lib.analysis.AnalysisResult
import de.tadris.flang_lib.analysis.MoveJudgmentType

class AnalysisOverviewFragment : Fragment() {

    private var _binding: FragmentAnalysisOverviewBinding? = null
    private val binding get() = _binding!!

    private var analysisResult: AnalysisResult? = null
    private val analysisViewModel: AnalysisViewModel by activityViewModels()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentAnalysisOverviewBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupViewModelObservers()
        updateDisplay()
    }
    
    private fun setupViewModelObservers() {
        analysisViewModel.analysisResult.observe(viewLifecycleOwner) { result ->
            analysisResult = result
            updateDisplay()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }


    private fun updateDisplay() {
        val result = analysisResult ?: return

        // Game summary
        binding.totalMovesText.text = result.totalMoves.toString()
        binding.analysisDepthText.text = result.analysisDepth.toString()
        binding.analysisTimeText.text = "${result.analysisTimeMs / 1000}s"

        // White player accuracy
        with(result.whiteAccuracy) {
            binding.whiteAccuracyText.text = String.format("%.1f%%", accuracy)
            binding.whiteGradeText.text = AccuracyCalculator.getAccuracyGrade(accuracy)
            val errorCount = inaccuracies + mistakes + blunders
            binding.whiteErrorsText.text = getString(R.string.analysisErrors, errorCount)
        }

        // Black player accuracy
        with(result.blackAccuracy) {
            binding.blackAccuracyText.text = String.format("%.1f%%", accuracy)
            binding.blackGradeText.text = AccuracyCalculator.getAccuracyGrade(accuracy)
            val errorCount = inaccuracies + mistakes + blunders
            binding.blackErrorsText.text = getString(R.string.analysisErrors, errorCount)
        }

        // Move distribution separated by player color
        val allMoves = result.moves.filter { it.hasJudgment }
        val whiteMoves = allMoves.filter { it.isWhiteMove }
        val blackMoves = allMoves.filter { !it.isWhiteMove }
        
        val whiteJudgmentCounts = whiteMoves.groupingBy { it.judgment!!.type }.eachCount()
        val blackJudgmentCounts = blackMoves.groupingBy { it.judgment!!.type }.eachCount()

        // White player move quality
        binding.whiteExcellentText.text = (whiteJudgmentCounts[MoveJudgmentType.EXCELLENT] ?: 0).toString()
        binding.whiteGoodText.text = (whiteJudgmentCounts[MoveJudgmentType.GOOD] ?: 0).toString()
        binding.whiteInaccuraciesText.text = (whiteJudgmentCounts[MoveJudgmentType.INACCURACY] ?: 0).toString()
        binding.whiteMistakesText.text = (whiteJudgmentCounts[MoveJudgmentType.MISTAKE] ?: 0).toString()
        binding.whiteBlundersText.text = (whiteJudgmentCounts[MoveJudgmentType.BLUNDER] ?: 0).toString()
        binding.whiteMissText.text = (whiteJudgmentCounts[MoveJudgmentType.MISS] ?: 0).toString()
        
        // Black player move quality
        binding.blackExcellentText.text = (blackJudgmentCounts[MoveJudgmentType.EXCELLENT] ?: 0).toString()
        binding.blackGoodText.text = (blackJudgmentCounts[MoveJudgmentType.GOOD] ?: 0).toString()
        binding.blackInaccuraciesText.text = (blackJudgmentCounts[MoveJudgmentType.INACCURACY] ?: 0).toString()
        binding.blackMistakesText.text = (blackJudgmentCounts[MoveJudgmentType.MISTAKE] ?: 0).toString()
        binding.blackBlundersText.text = (blackJudgmentCounts[MoveJudgmentType.BLUNDER] ?: 0).toString()
        binding.blackMissText.text = (blackJudgmentCounts[MoveJudgmentType.MISS] ?: 0).toString()
    }

    companion object {
        fun newInstance(): AnalysisOverviewFragment {
            return AnalysisOverviewFragment()
        }
    }
}