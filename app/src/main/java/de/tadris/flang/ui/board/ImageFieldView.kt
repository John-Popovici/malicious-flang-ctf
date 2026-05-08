package de.tadris.flang.ui.board

import android.content.Context

abstract class ImageFieldView(context: Context) : androidx.appcompat.widget.AppCompatImageView(context), FieldView {

    override fun getView() = this

}