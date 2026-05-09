package de.tadris.flang_lib.analysis

/**
 * Enum representing different types of move judgment comments that can be localized.
 * Each comment type corresponds to a specific evaluation reason.
 */
enum class MoveJudgmentComment {
    // Standard move evaluations
    BEST_MOVE,
    GOOD_MOVE,
    INACCURACY,
    MISTAKE,
    BLUNDER,
    MAJOR_BLUNDER,
    BOOK_MOVE,
    FORCED_MOVE,
    RESIGN,
    MISS,
    
    // Special situations
    MISSED_FORCED_MATE,
    ALLOWS_MATE,
    MAJOR_MATERIAL_LOSS,

    // Pre-mate situations
    DEFENDING_PERFECTLY,           // Best defense in losing position
    DEFENDING_AGAINST_MATE,        // Black plays reasonable defense when mate is forced
    FASTER_MATE_AVAILABLE,         // White could have mated faster
}