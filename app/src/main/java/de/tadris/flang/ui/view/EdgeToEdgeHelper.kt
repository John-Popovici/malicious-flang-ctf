package de.tadris.flang.ui.view

import android.view.View
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import de.tadris.flang.R

fun View.addTopPadding(){
    ViewCompat.setOnApplyWindowInsetsListener(this) { v, insets ->
        val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
        v.setPadding(0, systemBars.top, 0, 0)
        insets
    }
}

fun View.addBottomPadding(){
    ViewCompat.setOnApplyWindowInsetsListener(this) { v, insets ->
        val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
        val ime = insets.getInsets(WindowInsetsCompat.Type.ime())
        v.setPadding(0, 0, 0, maxOf(systemBars.bottom, ime.bottom))
        insets
    }
}