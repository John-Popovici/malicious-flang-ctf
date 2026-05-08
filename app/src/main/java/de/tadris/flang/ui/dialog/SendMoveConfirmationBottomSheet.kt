package de.tadris.flang.ui.dialog

import android.content.Context
import androidx.core.content.ContextCompat
import de.tadris.flang.R

class SendMoveConfirmationBottomSheet(context: Context) : ConfirmationBottomSheet(context) {

    fun show(onConfirm: () -> Unit, onCancel: (() -> Unit)? = null) {
        show(
            icon = "📨",
            titleRes = R.string.confirmSendMoveTitle,
            messageRes = R.string.confirmSendMoveMessage,
            confirmButtonTextRes = R.string.confirmSendMove,
            confirmButtonColor = ContextCompat.getColor(context, R.color.green_400),
            onConfirm = onConfirm,
            onCancel = onCancel
        )
    }
}