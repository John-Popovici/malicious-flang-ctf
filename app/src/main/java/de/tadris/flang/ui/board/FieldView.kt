package de.tadris.flang.ui.board

import android.view.View
import de.tadris.flang_lib.BoardIndex

interface FieldView {

    fun isFullOverlay(): Boolean = false

    fun getLocation(): BoardIndex

    fun getView(): View

}