package de.tadris.flang.network_api.util

import com.google.gson.Gson
import com.google.gson.GsonBuilder
import de.tadris.flang_lib.analysis.AnalysisResult

object AnalysisSerializer {
    
    private val gson: Gson = GsonBuilder()
        .serializeNulls()
        .create()

    fun serialize(result: AnalysisResult): String {
        return gson.toJson(result)
    }

    fun deserialize(json: String): AnalysisResult {
        return gson.fromJson(json, AnalysisResult::class.java)
    }

}