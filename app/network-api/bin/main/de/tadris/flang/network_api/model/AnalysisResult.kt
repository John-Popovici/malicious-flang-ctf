package de.tadris.flang.network_api.model

data class AnalysisResult(
    val id: Long,
    val fmn: String,
    val dateStarted: String,
    val dateEnded: String?,
    val isFinished: Boolean,
    val progress: Double,
    val data: String?
)