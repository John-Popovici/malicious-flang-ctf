package de.tadris.flang.ui.fragment.analysis

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import com.github.mikephil.charting.data.Entry
import com.github.mikephil.charting.data.LineData
import com.github.mikephil.charting.data.LineDataSet
import com.github.mikephil.charting.highlight.Highlight
import com.github.mikephil.charting.listener.OnChartValueSelectedListener
import de.tadris.flang.R
import de.tadris.flang.databinding.FragmentAnalysisChartBinding
import de.tadris.flang.ui.view.ChartFormatter
import de.tadris.flang.util.getThemePrimaryColor
import de.tadris.flang_lib.analysis.AnalysisResult
import de.tadris.flang_lib.analysis.MoveInfo
import de.tadris.flang_lib.analysis.PositionEvaluation
import kotlin.math.max

class AnalysisChartFragment : Fragment(), OnChartValueSelectedListener {

    private var _binding: FragmentAnalysisChartBinding? = null
    private val binding get() = _binding!!

    private var analysisResult: AnalysisResult? = null
    
    private val analysisViewModel: AnalysisViewModel by activityViewModels()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentAnalysisChartBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        with(binding.computerAnalysisChart) {
            with(ChartFormatter) {
                initChart(requireActivity())
            }
            setOnChartValueSelectedListener(this@AnalysisChartFragment)
        }

        setupViewModelObservers()
        updateChart()
    }
    
    private fun setupViewModelObservers() {
        analysisViewModel.analysisResult.observe(viewLifecycleOwner) { result ->
            analysisResult = result
            updateChart()
        }
        
        analysisViewModel.currentMoveIndex.observe(viewLifecycleOwner) { moveIndex ->
            highlightMove(moveIndex)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    private fun highlightMove(moveIndex: Int) {
        if (_binding != null && binding.computerAnalysisChart.data != null) {
            binding.computerAnalysisChart.highlightValue(moveIndex.toFloat(), 0)
        }
    }

    private fun updateChart() {
        val result = analysisResult ?: return
        val chart = binding.computerAnalysisChart

        val evalDataSet = getEvaluationDataSet(result)

        val scoreData = LineData(evalDataSet)
        scoreData.setDrawValues(false)

        chart.description.text = ""
        chart.data = scoreData
        chart.legend.setCustom(emptyArray())

        chart.axisLeft.axisMinimum = -21f
        chart.axisLeft.axisMaximum = 21f
        chart.axisRight.axisMinimum = -21f
        chart.axisRight.axisMaximum = 21f

        chart.isHighlightPerDragEnabled = true
        chart.isHighlightPerTapEnabled = true

        chart.notifyDataSetChanged()
        chart.invalidate()
    }

    private fun getEvaluationDataSet(result: AnalysisResult): LineDataSet {
        val entries = result.moves.mapIndexed { index, moveInfo ->
            val currentEval = moveInfo.evaluation.getNormalizedEval()
            val lastEval = if(index > 0) result.moves[index - 1].evaluation.getNormalizedEval() else null

            val displayedEval = if(lastEval != null) (currentEval + lastEval) / 2 else currentEval
            Entry(index.toFloat(), displayedEval, moveInfo)
        }

        val dataSet = LineDataSet(entries, "Evaluation")
        with(dataSet) {
            setDrawCircles(false)
            color = requireContext().resources.getColor(R.color.colorPrimary, null)
            lineWidth = 3f
            highLightColor = requireActivity().getThemePrimaryColor()
            highlightLineWidth = 2f
            setDrawFilled(true)
            fillAlpha = 30
        }

        return dataSet
    }

    private fun PositionEvaluation.getNormalizedEval() =
        (centipawns.toFloat() / 10f).coerceIn(-20f, 20f)

    override fun onValueSelected(e: Entry, h: Highlight) {
        val moveInfo = e.data as? MoveInfo
        analysisViewModel.setCurrentMoveIndex(h.x.toInt())
    }

    override fun onNothingSelected() {
        // Do nothing
    }

    companion object {
        fun newInstance(): AnalysisChartFragment {
            return AnalysisChartFragment()
        }
    }
}