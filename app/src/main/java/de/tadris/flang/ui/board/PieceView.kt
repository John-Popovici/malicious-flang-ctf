package de.tadris.flang.ui.board

import android.content.Context
import androidx.annotation.DrawableRes
import de.tadris.flang.R
import de.tadris.flang_lib.TYPE_FLANGER
import de.tadris.flang_lib.TYPE_HORSE
import de.tadris.flang_lib.TYPE_KING
import de.tadris.flang_lib.TYPE_PAWN
import de.tadris.flang_lib.TYPE_ROOK
import de.tadris.flang_lib.TYPE_UNI
import de.tadris.flang_lib.BoardIndex
import de.tadris.flang_lib.PieceState
import de.tadris.flang_lib.TYPE_RIDER
import de.tadris.flang_lib.getColor
import de.tadris.flang_lib.getFrozen
import de.tadris.flang_lib.getType

class PieceView(context: Context, private var location: BoardIndex, private var state: PieceState, private var rotateBlackPieces: Boolean = false) : ImageFieldView(context) {

    init {
        setImageResource(getImageByPiece(state))
        applyRotation()
        refresh()
    }

    override fun getLocation(): BoardIndex {
        return location
    }

    fun getPiece(): PieceState {
        return state
    }

    fun setPiece(location: BoardIndex, piece: PieceState){
        this.location = location
        this.state = piece
        applyRotation()
        refresh()
    }
    
    private fun applyRotation() {
        rotation = if (rotateBlackPieces && !state.getColor()) 180f else 0f
    }

    fun refresh(){
        if(state.getFrozen()){
            setBackgroundResource(R.color.frozen)
        }else{
            setBackgroundColor(0x0)
        }
    }

    companion object {

        @DrawableRes
        fun getImageByPiece(piece: PieceState): Int{
            return when(piece.getColor()){
                false -> when(piece.getType()){
                    TYPE_PAWN -> R.drawable.bp
                    TYPE_HORSE -> R.drawable.bn
                    TYPE_ROOK -> R.drawable.br
                    TYPE_UNI -> R.drawable.bq
                    TYPE_FLANGER -> R.drawable.bf
                    TYPE_KING -> R.drawable.bk
                    TYPE_RIDER -> R.drawable.bm
                    else -> R.drawable.bk
                }
                true -> when(piece.getType()){
                    TYPE_PAWN -> R.drawable.wp
                    TYPE_HORSE -> R.drawable.wn
                    TYPE_ROOK -> R.drawable.wr
                    TYPE_UNI -> R.drawable.wq
                    TYPE_FLANGER -> R.drawable.wf
                    TYPE_KING -> R.drawable.wk
                    TYPE_RIDER -> R.drawable.wm
                    else -> R.drawable.wk
                }
            }
        }
    }

}