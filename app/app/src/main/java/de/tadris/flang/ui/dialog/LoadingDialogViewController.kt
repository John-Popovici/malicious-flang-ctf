package de.tadris.flang.ui.dialog

import android.app.AlertDialog
import android.content.Context
import android.widget.TextView
import de.tadris.flang.R

class LoadingDialogViewController(context: Context) {

    private val dialog: AlertDialog = AlertDialog.Builder(context)
        .setView(R.layout.dialog_loading)
        .setCancelable(false)
        .create()

    init {
        show()
    }

    fun show(): LoadingDialogViewController{
        dialog.show()
        return this
    }

    fun setText(text: String){
        dialog.findViewById<TextView>(R.id.loadingText).text = text
    }

    fun hide(){
        dialog.cancel()
    }

}