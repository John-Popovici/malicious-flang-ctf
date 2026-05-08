package de.tadris.flang_lib.analysis

import de.tadris.flang_lib.bot.BotResult

data class FullEvaluationData(
    val gameFMN: String,
    val evaluations: List<BotResult>,
)