package de.tadris.flang.ui.dialog

import android.content.Context
import androidx.core.content.ContextCompat
import de.tadris.flang.R

class ResignConfirmationBottomSheet(context: Context) : ConfirmationBottomSheet(context) {

    fun show(onConfirm: () -> Unit, onCancel: (() -> Unit)? = null) {
        show(
            icon = "⚠️",
            titleRes = R.string.confirmResignTitle,
            messageRes = R.string.confirmResignMessage,
            confirmButtonTextRes = R.string.confirmResign,
            confirmButtonColor = ContextCompat.getColor(context, R.color.red_700),
            onConfirm = onConfirm,
            onCancel = onCancel
        )
    }
}