package de.tadris.flang.ui.board

import android.content.Context
import android.util.Log
import de.tadris.flang_lib.Board
import de.tadris.flang_lib.BoardIndex
import de.tadris.flang_lib.Color
import de.tadris.flang_lib.Move
import de.tadris.flang_lib.getColor
import de.tadris.flang_lib.getFromIndex
import de.tadris.flang_lib.getFromPieceState
import de.tadris.flang_lib.getToIndex
import de.tadris.flang_lib.isEmpty
import de.tadris.flang_lib.packMove

class BoardMoveDetector(
    private val context: Context,
    private val boardView: BoardView,
    private val myColor: Color?,
    var listener: MoveListener?,
) : BoardView.FieldClickListener {

    private var movesAllowed = true
    private var premovesAllowed = true

    var selected: MiscView? = null
    var options: MutableList<MiscView> = mutableListOf()

    override fun onFieldTouchBegin(board: Board, location: BoardIndex) {
        Log.d("MoveDetector", "Touch begin: $location. movesAllowed: $movesAllowed premovesAllowed: $premovesAllowed")
        val piece = board.getAt(location)
        if(!movesAllowed && !premovesAllowed){
            return
        }
        if(selected == null){
            if(!piece.isEmpty() && !board.gameIsComplete()){
                if(piece.getColor() == board.atMove){
                    if(myColor == null || (board.atMove == myColor && myColor == piece.getColor())){
                        Log.d("MoveDetector", "Selecting $piece")
                        selectPiece(board, location)
                    }
                }else if(premovesAllowed && myColor != board.atMove && myColor == piece.getColor()){ // opponent is at move and I clicked on my piece
                    Log.d("MoveDetector", "Selecting $piece (premove)")
                    selectPiece(board, location)
                }
            }
        }else{
            val clickedOnSelected = selected!!.getLocation() == location
            if(options.find { it.getLocation() == location } == null){
                deselect()
                if(!piece.isEmpty() && !clickedOnSelected){
                    onFieldTouchBegin(board, location)
                }
            }
        }
    }

    private fun selectPiece(board: Board, location: BoardIndex){
        listener?.onPremoveClearRequested()
        selected = MiscView(context, location, MiscView.MiscType.SELECTED)
        boardView.attach(selected!!)
        options = mutableListOf()

        val piece = board.getAt(location)
        board.getMoves(piece.getColor()).filter { it.getFromIndex() == location }.forEach { move ->
            addOption(move.getToIndex())
        }
    }

    private fun addOption(target: BoardIndex){
        val view = MiscView(context, target, MiscView.MiscType.OPTION)
        options.add(view)
        boardView.attach(view)
    }

    private fun deselect(){
        if(selected == null){ return }
        boardView.detach(selected!!)
        options.forEach {
            boardView.detach(it)
        }
        selected = null
        options.clear()
    }

    override fun onFieldRelease(board: Board, location: BoardIndex) {
        options.toList().forEach {
            if(it.getLocation() == location){
                requestMove(board, packMove(board, selected!!.getLocation(), location))
            }
        }
    }

    private fun requestMove(board: Board, move: Move){
        deselect()
        if(board.atMove == move.getFromPieceState().getColor()){
            listener?.onMoveRequested(move)
        }else if(premovesAllowed){
            listener?.onPremoveRequested(move)
        }
    }

    fun setAllowed(allowed: Boolean, premoves: Boolean){
        this.movesAllowed = allowed
        this.premovesAllowed = premoves
        if(!allowed){
            deselect()
        }
    }

    interface MoveListener {

        fun onMoveRequested(move: Move)

        fun onPremoveRequested(move: Move)

        fun onPremoveClearRequested()

    }

}