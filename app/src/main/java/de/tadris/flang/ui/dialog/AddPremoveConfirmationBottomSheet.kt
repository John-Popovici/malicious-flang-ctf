package de.tadris.flang.ui.dialog

import android.content.Context
import androidx.core.content.ContextCompat
import de.tadris.flang.R
import de.tadris.flang_lib.Move
import de.tadris.flang_lib.getNotationV1

class AddPremoveConfirmationBottomSheet(context: Context) : ConfirmationBottomSheet(context) {

    fun show(move: Move, moveCount: Int, onConfirm: () -> Unit, onCancel: (() -> Unit)? = null) {
        val moveText = move.getNotationV1()
        val message = context.getString(R.string.confirmAddPremoveMessage, moveText, moveCount)

        show(
            icon = "⚡",
            title = context.getString(R.string.confirmAddPremoveTitle),
            message = message,
            confirmButtonText = context.getString(R.string.confirmAddPremove),
            confirmButtonColor = ContextCompat.getColor(context, R.color.colorPrimary),
            onConfirm = onConfirm,
            onCancel = onCancel
        )
    }

    fun show(
        move: Move,
        moveCount: Int,
        customMessage: String,
        onConfirm: () -> Unit,
        onCancel: (() -> Unit)? = null
    ) {
        show(
            icon = "⚡",
            title = context.getString(R.string.confirmAddPremoveTitle),
            message = customMessage,
            confirmButtonText = context.getString(R.string.confirmAddPremove),
            confirmButtonColor = ContextCompat.getColor(context, R.color.colorPrimary),
            onConfirm = onConfirm,
            onCancel = onCancel
        )
    }
}