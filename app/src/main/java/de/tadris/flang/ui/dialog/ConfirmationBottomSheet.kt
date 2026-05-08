package de.tadris.flang.ui.dialog

import android.content.Context
import android.view.LayoutInflater
import android.widget.TextView
import androidx.annotation.StringRes
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.button.MaterialButton
import de.tadris.flang.R

abstract class ConfirmationBottomSheet(protected val context: Context) {

    private var bottomSheetDialog: BottomSheetDialog? = null

    fun show(
        icon: String,
        @StringRes titleRes: Int,
        @StringRes messageRes: Int,
        @StringRes confirmButtonTextRes: Int,
        confirmButtonColor: Int? = null,
        onConfirm: () -> Unit,
        onCancel: (() -> Unit)? = null
    ) {
        show(
            icon = icon,
            title = context.getString(titleRes),
            message = context.getString(messageRes),
            confirmButtonText = context.getString(confirmButtonTextRes),
            confirmButtonColor = confirmButtonColor,
            onConfirm = onConfirm,
            onCancel = onCancel
        )
    }

    fun show(
        icon: String,
        title: String,
        message: String,
        confirmButtonText: String,
        confirmButtonColor: Int? = null,
        onConfirm: () -> Unit,
        onCancel: (() -> Unit)? = null
    ) {
        val bottomSheetView = LayoutInflater.from(context)
            .inflate(R.layout.bottom_sheet_confirmation, null)
        
        bottomSheetDialog = BottomSheetDialog(context).apply {
            setContentView(bottomSheetView)
            setCancelable(true)
        }
        
        // Setup UI elements
        bottomSheetView.findViewById<TextView>(R.id.confirmationIcon).text = icon
        bottomSheetView.findViewById<TextView>(R.id.confirmationTitle).text = title
        bottomSheetView.findViewById<TextView>(R.id.confirmationMessage).text = message

        val confirmButton = bottomSheetView.findViewById<MaterialButton>(R.id.btnConfirm)
        confirmButton.text = confirmButtonText
        
        // Set confirm button color if provided
        confirmButtonColor?.let { color ->
            confirmButton.backgroundTintList = android.content.res.ColorStateList.valueOf(color)
        }
        
        // Setup button listeners
        confirmButton.setOnClickListener {
            onConfirm()
            dismiss()
        }
        
        bottomSheetView.findViewById<MaterialButton>(R.id.btnCancel).setOnClickListener {
            onCancel?.invoke()
            dismiss()
        }

        bottomSheetDialog?.setOnCancelListener {
            onCancel?.invoke()
        }
        
        bottomSheetDialog?.show()
    }

    fun dismiss() {
        bottomSheetDialog?.dismiss()
        bottomSheetDialog = null
    }
}