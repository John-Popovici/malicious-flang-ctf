package de.tadris.flang.ui.fragment.analysis

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.recyclerview.widget.LinearLayoutManager
import de.tadris.flang.databinding.FragmentAnalysisMovesBinding
import de.tadris.flang_lib.analysis.AnalysisResult

class AnalysisMovesFragment : Fragment() {

    private var _binding: FragmentAnalysisMovesBinding? = null
    private val binding get() = _binding!!

    private lateinit var movesAdapter: AnalysisMovesAdapter
    private var analysisResult: AnalysisResult? = null
    
    private val analysisViewModel: AnalysisViewModel by activityViewModels()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentAnalysisMovesBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        setupRecyclerView()
        setupViewModelObservers()
        updateMoves()
    }
    
    private fun setupViewModelObservers() {
        analysisViewModel.analysisResult.observe(viewLifecycleOwner) { result ->
            Log.d("AnalysisMovesFragment", "Received analysis result: ${result != null}")
            analysisResult = result
            if (::movesAdapter.isInitialized) {
                updateMoves()
            }
        }
        
        analysisViewModel.currentMoveIndex.observe(viewLifecycleOwner) { moveIndex ->
            Log.d("AnalysisMovesFragment", "Received move index: $moveIndex")
            if (::movesAdapter.isInitialized && moveIndex >= 0 && moveIndex < movesAdapter.itemCount) {
                movesAdapter.setSelectedMoveIndex(moveIndex)
                binding.movesRecyclerView.scrollToPosition(moveIndex)
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    private fun setupRecyclerView() {
        movesAdapter = AnalysisMovesAdapter(analysisResult?.fmn) { moveIndex, _ ->
            analysisViewModel.setCurrentMoveIndex(moveIndex)
        }
        
        binding.movesRecyclerView.apply {
            layoutManager = LinearLayoutManager(context)
            adapter = movesAdapter
        }
    }

    private fun updateMoves() {
        val result = analysisResult ?: return
        movesAdapter.fmn = result.fmn
        movesAdapter.submitList(result.moves)
    }

    companion object {
        fun newInstance(): AnalysisMovesFragment {
            return AnalysisMovesFragment()
        }
    }
}