package de.tadris.flang.util

import android.content.Context
import android.text.Spanned
import android.text.style.RelativeSizeSpan
import android.view.View
import android.widget.TextView
import androidx.core.text.buildSpannedString
import de.tadris.flang.R
import de.tadris.flang.network_api.model.UserInfo
import kotlin.math.absoluteValue

fun UserInfo.getTitleColor(context: Context): Int {
    return if(isBot){
        context.resources.getColor(R.color.botTitleColor)
    }else{
        context.resources.getColor(R.color.defaultTitleColor)
    }
}

fun UserInfo.applyTo(titleText: TextView, nameText: TextView, ratingText: TextView? = null){
    nameText.text = buildSpannedString {
        val lines = username.lines()
        append(lines.first())
        if(lines.size > 1){
            val start = length
            append("\n")
            append(lines[1])
            setSpan(
                RelativeSizeSpan(0.7f), start, length, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
            )
        }
    }
    if(hasTitle()){
        titleText.visibility = View.VISIBLE
        titleText.text = getDisplayedTitle()
        titleText.setTextColor(getTitleColor(titleText.context))
    }else{
        titleText.visibility = View.GONE
    }
    ratingText?.text = getRatingText()
}

fun UserInfo.formatChatTextColor(nameText: TextView){
    val colors = listOf(
        R.color.chatPersonRed,
        R.color.chatPersonMagenta,
        R.color.chatPersonPurple,
        R.color.chatPersonBlue,
        R.color.chatPersonLightBlue,
        R.color.chatPersonGreen,
        R.color.chatPersonOrange
    )
    val colorRes = colors[username.hashCode().absoluteValue % colors.size]
    nameText.setTextColor(nameText.context.resources.getColor(colorRes))
}