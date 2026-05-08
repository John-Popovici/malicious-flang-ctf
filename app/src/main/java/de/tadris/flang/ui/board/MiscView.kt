package de.tadris.flang.ui.board

import android.content.Context
import androidx.annotation.DrawableRes
import de.tadris.flang.R
import de.tadris.flang_lib.BoardIndex

class MiscView(context: Context, private val location: BoardIndex, val type: MiscType) : ImageFieldView(context) {

    init {
        setImageResource(getImageByType(type))
    }

    override fun getLocation() = location

    companion object {

        @DrawableRes
        fun getImageByType(type: MiscType): Int {
            return when(type){
                MiscType.MOVED_TO -> R.color.fieldMove
                MiscType.MOVED_FROM -> R.color.fieldMove
                MiscType.OPTION -> R.drawable.ic_selected
                MiscType.SELECTED -> R.color.fieldSelected
            }
        }

    }

    enum class MiscType(val foreground: Boolean = false) {
        MOVED_FROM,
        MOVED_TO,
        OPTION(true),
        SELECTED;
    }

}