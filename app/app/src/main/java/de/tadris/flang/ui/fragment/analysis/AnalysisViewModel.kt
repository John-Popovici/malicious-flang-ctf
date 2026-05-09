package de.tadris.flang.ui.fragment.analysis

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import de.tadris.flang_lib.analysis.AnalysisResult

class AnalysisViewModel : ViewModel() {
    
    private val _analysisResult = MutableLiveData<AnalysisResult?>()
    val analysisResult: LiveData<AnalysisResult?> = _analysisResult
    
    private val _currentMoveIndex = MutableLiveData<Int>()
    val currentMoveIndex: LiveData<Int> = _currentMoveIndex

    fun clearData(){
        setAnalysisResult(null)
        setCurrentMoveIndex(0)
    }
    
    fun setAnalysisResult(result: AnalysisResult?) {
        _analysisResult.value = result
    }
    
    fun setCurrentMoveIndex(index: Int) {
        if (_currentMoveIndex.value != index) {
            _currentMoveIndex.value = index
        }
    }

}