package de.tadris.flang.network_api.model

data class AnalysisQuota(
    val dailyLimit: Int,
    val used: Int,
    val remaining: Int
)