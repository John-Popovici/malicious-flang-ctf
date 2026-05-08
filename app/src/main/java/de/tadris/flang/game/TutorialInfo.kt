package de.tadris.flang.game

import androidx.annotation.StringRes
import de.tadris.flang.R
import de.tadris.flang_lib.COLOR_BLACK
import de.tadris.flang_lib.TYPE_UNI
import de.tadris.flang_lib.COLOR_WHITE
import de.tadris.flang_lib.Board
import de.tadris.flang_lib.getType
import de.tadris.flang_lib.winningY
import de.tadris.flang_lib.y

enum class TutorialInfo(
    val index: Int,
    @StringRes val title: Int,
    @StringRes val description: Int,
    val fbn: String,
    val freezeEnabled: Boolean = true,
    val clickable: Boolean = true,
    val hintsEnabled: Boolean = false,
    val finish: Boolean = false,
    val botTurns: Boolean = true,
    val goal: Goal = Goal.NONE
) {

    INTRODUCTION(0, R.string.tutorialIntroductionTitle, R.string.tutorialIntroductionMessage,
        Board.DEFAULT_FBN2,
            clickable = false),
    KING(1, R.string.tutorialKingTitle, R.string.tutorialKingMessage,
            "        " +
            "  K     " +
            "        " +
            "        " +
            "        " +
            "    p   " +
            "        " +
            "        ",
            freezeEnabled = false,
            botTurns = false,
            goal = Goal.CAPTURE),
    PAWN(2, R.string.tutorialPawnTitle, R.string.tutorialPawnMessage,
            "        " +
            "  P     " +
            "        " +
            "        " +
            "        " +
            "        " +
            "        " +
            "        ",
            freezeEnabled = false,
            botTurns = false,
            goal = Goal.PROMOTE_A_UNI),
    ROOK(3, R.string.tutorialRookTitle, R.string.tutorialRookMessage,
            "        " +
                    "      R " +
                    "        " +
                    "        " +
                    "  p     " +
                    "        " +
                    "        " +
                    "        ",
            freezeEnabled = false,
            botTurns = false,
            goal = Goal.CAPTURE),
    HORSE(4, R.string.tutorialHorseTitle, R.string.tutorialHorseMessage,
            "        " +
                    "      H " +
                    "        " +
                    "        " +
                    "   p    " +
                    "        " +
                    "        " +
                    "        ",
            freezeEnabled = false,
            botTurns = false,
            goal = Goal.CAPTURE),
    UNI(5, R.string.tutorialUniTitle, R.string.tutorialUniMessage,
            "        " +
                    " p    U " +
                    "        " +
                    "        " +
                    "   p  p " +
                    "        " +
                    "        " +
                    "   p    ",
            freezeEnabled = false,
            botTurns = false,
            goal = Goal.CAPTURE),
    FLANGER(6, R.string.tutorialFlangerTitle, R.string.tutorialFlangerMessage,
            "        " +
                    "      F " +
                    "        " +
                    "      p " +
                    "        " +
                    "p       " +
                    "     p  " +
                    "        ",
            freezeEnabled = false,
            botTurns = false,
            goal = Goal.CAPTURE),
    FREEZE(7, R.string.tutorialFreezeTitle, R.string.tutorialFreezeMessage,
            "        " +
                    "  PPP F " +
                    "        " +
                    "      p " +
                    "        " +
                    "p       " +
                    "     p  " +
                    "        ",
            goal = Goal.CAPTURE,
            botTurns = false),
    WIN_BASE(8, R.string.tutorialWinBaseTitle, R.string.tutorialWinBaseMessage,
            "        " +
                    " KPPP   " +
                    "        " +
                    "        " +
                    "        " +
                    "        " +
                    "        " +
                    "        ",
            goal = Goal.WIN_BASELINE,
            botTurns = false),
    WIN_CAPTURE(9, R.string.tutorialWinCaptureTitle, R.string.tutorialWinCaptureMessage,
            " U      " +
                    "  PPP   " +
                    "        " +
                    "   k    " +
                    "        " +
                    "        " +
                    "        " +
                    "        ",
            goal = Goal.WIN_CAPTURE,
            botTurns = false),
    COMPLETE(10, R.string.tutorialCompleteTitle, R.string.tutorialCompleteMessage,
        Board.DEFAULT_FBN2,
            goal = Goal.NONE, finish = true, hintsEnabled = true),



    ;

    fun toBoard() = Board.fromFBN(fbn)

    companion object {

        fun findByIndex(index: Int): TutorialInfo {
            return entries.find { it.index == index } ?: INTRODUCTION
        }

    }

    enum class Goal(@StringRes val message: Int, val condition: (Board) -> Boolean) {
        NONE(-1, { true }),
        PROMOTE_A_UNI(R.string.tutorialTargetReachLastRow, { board: Board ->
            var hasUni = false
            board.eachPiece(null) { _, state ->
                if(state.getType() == TYPE_UNI){
                    hasUni = true
                }
            }
            hasUni
        }),
        CAPTURE(R.string.tutorialTargetCapture, { board: Board ->
            var opponentHasPiece = false
            board.eachPiece(COLOR_BLACK) { _, _ ->
                opponentHasPiece = true
            }
            !opponentHasPiece
        }),
        WIN_BASELINE(R.string.tutorialTargetWin, { board: Board ->
            board.findKingIndex(COLOR_WHITE).y == COLOR_WHITE.winningY
        }),
        WIN_CAPTURE(R.string.tutorialTargetWin, { board: Board ->
            board.findKingIndex(COLOR_BLACK) == -1
        }),
    }

}