package de.tadris.flang.util

import android.app.Activity
import android.content.Context
import android.util.TypedValue
import androidx.annotation.AttrRes
import de.tadris.flang.R

fun Activity.getThemePrimaryColor(): Int {
    return getThemeColor(R.attr.colorPrimary)
}

fun Activity.getThemeSecondaryColor(): Int {
    return getThemeColor(R.attr.colorSecondary)
}

fun Activity.getThemePrimaryDarkColor(): Int {
    return getThemeColor(android.R.attr.colorPrimaryDark)
}

fun Activity.getThemeTextColor(): Int {
    return getThemeColor(android.R.attr.textColorPrimary)
}

fun Activity.getThemeTextColorInverse(): Int {
    return -0x1 - getThemeTextColor() or -0x1000000
}

fun Activity.getThemeColor(@AttrRes colorRes: Int): Int {
    val value = TypedValue()
    theme.resolveAttribute(colorRes, value, true)
    return value.data
}