package de.tadris.flang.ui.dialog

import android.content.ClipboardManager
import android.content.Context
import android.widget.EditText
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import de.tadris.flang.R
import de.tadris.flang_lib.Game
import de.tadris.flang_lib.Board

typealias ImportListener = (gameString: String, game: Game, type: ImportType) -> Unit

fun Fragment.openImportDialog(onImport: ImportListener){
    val editText = EditText(requireContext())
    editText.setText(readFromClipboard())
    AlertDialog.Builder(requireActivity())
        .setTitle(R.string.importGame)
        .setView(editText)
        .setPositiveButton(R.string.okay) { _, _ ->
            try{
                importAndShowGame(editText.text.toString().trim(), onImport)
            }catch (e: Exception){
                e.printStackTrace()
                AlertDialog.Builder(requireActivity())
                    .setTitle(R.string.importFailed)
                    .setMessage(e.message)
                    .setPositiveButton(R.string.okay, null)
                    .show()
            }
        }
        .show()
}

fun Fragment.readFromClipboard(): String {
    val clipboard = context?.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager? ?: return ""
    return clipboard.primaryClip?.getItemAt(0)?.text?.toString() ?: ""
}

private fun importAndShowGame(gameString: String, onImport: ImportListener){
    if(gameString.startsWith("+") || gameString.startsWith("-")){
        try {
            onImport(gameString, Game(initialBoard = Board.fromFBN(gameString)), ImportType.FBN2)
        }catch (e: Exception){
            // could be FBN1 with stripped space
            onImport(gameString, Game(initialBoard = Board.fromFBN(" $gameString")), ImportType.FBN2)
        }
    }else{
        onImport(gameString, Game.fromFMN(gameString), ImportType.FMN)
    }
}

enum class ImportType {
    FBN2,
    FMN
}