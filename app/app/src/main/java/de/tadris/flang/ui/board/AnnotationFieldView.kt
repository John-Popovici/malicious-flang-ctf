package de.tadris.flang.ui.board

import android.content.Context
import de.tadris.flang_lib.BoardIndex

class AnnotationFieldView(context: Context, private val location: BoardIndex, text: String) : androidx.appcompat.widget.AppCompatTextView(context), FieldView {

    init {
        setText(text)
    }

    override fun getLocation() = location

    override fun getView() = this

}