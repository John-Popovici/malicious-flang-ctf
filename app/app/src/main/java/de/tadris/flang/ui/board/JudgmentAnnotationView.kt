package de.tadris.flang.ui.board

import android.content.Context
import android.graphics.Typeface
import android.view.Gravity
import android.widget.FrameLayout
import androidx.appcompat.widget.AppCompatTextView
import de.tadris.flang.R
import de.tadris.flang_lib.analysis.MoveJudgmentType
import de.tadris.flang_lib.BoardIndex

class JudgmentAnnotationView(
    context: Context,
    private val location: BoardIndex,
    private val judgmentType: MoveJudgmentType
) : FrameLayout(context), FieldView {

    init {
        setupView()
    }

    private fun setupView() {
        // Create the inner TextView
        val textView = AppCompatTextView(context).apply {
            text = getJudgmentSymbol(judgmentType)
            textSize = 10f
            typeface = Typeface.DEFAULT_BOLD
            gravity = Gravity.CENTER
            setTextColor(context.resources.getColor(R.color.white, null))
            
            // Make background semi-transparent
            background = context.getDrawable(R.drawable.circle_background)?.apply {
                setTint(getJudgmentColor(judgmentType))
                alpha = 200 // Semi-transparent
            }
            
            val padding = 3
            setPadding(padding, padding, padding, padding)
        }
        
        // Add the TextView to the FrameLayout positioned in top-right corner
        val size = (context.resources.displayMetrics.density * 20).toInt() // 20dp
        val margin = (context.resources.displayMetrics.density * 2).toInt() // 2dp margin
        
        val layoutParams = LayoutParams(size, size).apply {
            gravity = Gravity.TOP or Gravity.START
            setMargins(0, margin, margin, 0)
        }
        
        addView(textView, layoutParams)
    }

    private fun getJudgmentSymbol(type: MoveJudgmentType): String {
        return when (type) {
            MoveJudgmentType.EXCELLENT -> "⭐"
            MoveJudgmentType.GOOD -> "✓"
            MoveJudgmentType.INACCURACY -> "?!"
            MoveJudgmentType.MISTAKE, MoveJudgmentType.MISS -> "?"
            MoveJudgmentType.BLUNDER -> "??"
            MoveJudgmentType.BOOK -> "📖"
            MoveJudgmentType.FORCED -> "□"
            MoveJudgmentType.RESIGN -> "#"
        }
    }

    private fun getJudgmentColor(type: MoveJudgmentType): Int {
        val colorRes = when (type) {
            MoveJudgmentType.EXCELLENT -> R.color.judgment_excellent_text
            MoveJudgmentType.GOOD -> R.color.judgment_good_text
            MoveJudgmentType.INACCURACY -> R.color.judgment_inaccuracy_text
            MoveJudgmentType.MISTAKE -> R.color.judgment_mistake_text
            MoveJudgmentType.MISS -> R.color.judgment_mistake_text
            MoveJudgmentType.BLUNDER -> R.color.judgment_blunder_text
            MoveJudgmentType.BOOK -> R.color.judgment_book_text
            MoveJudgmentType.FORCED -> R.color.judgment_forced_text
            MoveJudgmentType.RESIGN -> R.color.judgment_forced_text
        }
        return context.resources.getColor(colorRes, null)
    }

    override fun getView() = this

    override fun getLocation() = location
}