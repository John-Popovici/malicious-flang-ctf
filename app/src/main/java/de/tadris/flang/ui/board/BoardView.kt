package de.tadris.flang.ui.board

import android.animation.Animator
import android.annotation.SuppressLint
import android.graphics.Color
import android.graphics.Typeface
import android.os.Handler
import android.view.MotionEvent
import android.view.ViewGroup
import android.view.animation.*
import de.tadris.flang_lib.Board
import de.tadris.flang_lib.BoardIndex
import de.tadris.flang_lib.PieceState
import de.tadris.flang_lib.getColor
import de.tadris.flang_lib.getType
import de.tadris.flang_lib.indexOf
import de.tadris.flang_lib.isEmpty
import de.tadris.flang_lib.x
import de.tadris.flang_lib.y

@SuppressLint("ClickableViewAccessibility")
class BoardView(val rootView: ViewGroup, board: Board, isClickable: Boolean, val animate: Boolean, private var rotateBlackPieces: Boolean = false) {

    var board: Board = board
        private set

    private val views = mutableListOf<FieldView>()
    private var isFlipped = false
    var listener: FieldClickListener? = null

    init {
        rootView.removeAllViews()
        if(isClickable){
            rootView.setOnTouchListener { _, event ->
                val location = unproject(event.x, event.y)
                if (event.actionMasked == MotionEvent.ACTION_DOWN) {
                    listener?.onFieldTouchBegin(this.board, location)
                } else if (event.actionMasked == MotionEvent.ACTION_UP) {
                    listener?.onFieldRelease(this.board, location)
                }
                true
            }
        }
    }

    fun setBoard(board: Board){
        detachAll()
        this.board = board
        board.eachLocation { index ->
            val piece = board.getAt(index)
            if(!piece.isEmpty()){
                attach(getPieceView(index, piece))
            }
        }
    }

    fun refreshBoard(board: Board){
        this.board = board
        refresh()
    }

    fun refresh(){
        var changes = 0

        val viewList = views.filterIsInstance<PieceView>().toMutableList()

        val oldViews = mutableListOf<PieceView>()
        val newPieces = mutableListOf<Pair<BoardIndex, PieceState>>()

        board.eachLocation { index ->
            val pieceState = board.getAt(index)
            val view = viewList.find { it.getLocation() == index }
            if(!pieceState.isEmpty() && view != null){
                val pieceIsSame = view.getPiece().getColor() == pieceState.getColor() && view.getPiece().getType() == pieceState.getType()
                if (pieceIsSame) {
                    view.setPiece(index, pieceState)
                } else {
                    // Piece changed
                    oldViews.add(view)
                    newPieces.add(index to pieceState)
                }
            }else if(!pieceState.isEmpty() && view == null){
                // Piece got here
                newPieces.add(index to pieceState)
            }else if(pieceState.isEmpty() && view != null){
                // Piece moved away
                oldViews.add(view)
            }
        }

        newPieces.toList().forEach { (index, piece) ->
            val view = oldViews.find {
                it.getPiece().getColor() == piece.getColor() && it.getPiece().getType() == piece.getType()
            }
            if(view != null){
                view.setPiece(index, piece)
                if(animate){
                    animateTo(view, index)
                }
                oldViews.remove(view)
            }else{
                attach(getPieceView(index, piece))
            }
            changes++
        }

        oldViews.forEach {
            detach(it)
            changes++
        }
    }

    fun showMessage(message: String, duration: Long){
        val text = AnnotationFieldView(rootView.context, 0, message)
        text.textSize = 48f
        text.typeface = Typeface.DEFAULT_BOLD
        text.setTextColor(Color.BLACK)

        rootView.addView(text, -1, ViewGroup.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT))
        views.add(text)

        text.y = getViewSize() / 2f
        text.x = -getViewSize().toFloat()

        Handler().postDelayed({
            text.x = -text.width.toFloat()
            text.animate()
                    .setDuration(500)
                    .setInterpolator(OvershootInterpolator())
                    .translationX((getViewSize() - text.width) / 2f)
                    .setListener(object : Animator.AnimatorListener {
                        override fun onAnimationStart(animation: Animator) {}
                        override fun onAnimationCancel(animation: Animator) {}
                        override fun onAnimationRepeat(animation: Animator) {}

                        override fun onAnimationEnd(animation: Animator) {
                            text.clearAnimation()
                            text.animate()
                                    .setStartDelay(duration)
                                    .setDuration(1000)
                                    .setInterpolator(AccelerateInterpolator())
                                    .translationX(getViewSize() * 2f)
                                    .setListener(object : Animator.AnimatorListener {
                                        override fun onAnimationStart(animation: Animator) {}
                                        override fun onAnimationCancel(animation: Animator) {}
                                        override fun onAnimationRepeat(animation: Animator) {}

                                        override fun onAnimationEnd(animation: Animator) {
                                            detach(text)
                                        }
                                    })
                                    .start()
                        }
                    })
                    .start()
        }, 200)
    }

    fun attach(fieldView: FieldView){
        val view = fieldView.getView()
        if(fieldView.isFullOverlay()){
            rootView.addView(view, 0, ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT))
        }else{
            rootView.addView(view, if((fieldView is MiscView && !fieldView.type.foreground)) 0 else -1, ViewGroup.LayoutParams(getScale().toInt(), getScale().toInt()))
            val vector = project(fieldView.getLocation())
            view.x = getScale() * vector.x
            view.y = getScale() * vector.y
        }
        views.add(fieldView)
        if(animate){
            view.alpha = 0f
            view.animate()
                .alpha(1f)
                .setDuration(200)
                .start()
        }
    }

    fun animateTo(fieldView: ImageFieldView, location: BoardIndex){
        val vector = project(fieldView.getLocation())

        fieldView.clearAnimation()
        fieldView.animate()
            .setDuration(250)
            .setInterpolator(DecelerateInterpolator())
            .translationX(getScale() * vector.x)
            .translationY(getScale() * vector.y)
            .start()
    }

    fun project(index: BoardIndex): BoardIndex {
        return if(!isFlipped){
            indexOf(index.x, Board.BOARD_SIZE - 1 - index.y)
        }else{
            indexOf(Board.BOARD_SIZE - 1 - index.x, index.y)
        }
    }

    fun unproject(x: Float, y: Float): BoardIndex {
        val fieldX = (x / getScale()).toInt()
        val fieldY = (y / getScale()).toInt()
        return if(isFlipped){
            indexOf(Board.BOARD_SIZE - 1 - fieldX, fieldY)
        }else{
            indexOf(fieldX, Board.BOARD_SIZE - 1 - fieldY)
        }
    }

    fun detachAllAnnotations(){
        views.filterIsInstance<AnnotationFieldView>().forEach {
            detach(it)
        }
        views.filterIsInstance<JudgmentAnnotationView>().forEach {
            detach(it)
        }
    }

    fun detachAllArrows(){
        views.filterIsInstance<ArrowFieldView>().forEach {
            detach(it)
        }
    }

    fun detachAll(){
        rootView.removeAllViews()
        views.clear()
    }

    fun detach(fieldView: FieldView){
        if(animate){
            fadeOutAndRemove(fieldView)
        }else{
            rootView.removeView(fieldView.getView())
        }
        views.remove(fieldView)
    }

    private fun fadeOutAndRemove(fieldView: FieldView){
        fieldView.getView().clearAnimation()
        fieldView.getView()
            .animate()
            .alpha(0f)
            .setDuration(200)
            .setListener(AnimatorEndListener {
                rootView.removeView(fieldView.getView())
            })
            .start()
    }

    fun setFlipped(flipped: Boolean){
        if(isFlipped == flipped) return
        isFlipped = flipped
        setBoard(board)
    }

    fun isFlipped(): Boolean {
        return isFlipped
    }
    
    fun setBlackPiecesRotated(rotated: Boolean) {
        rotateBlackPieces = rotated
        setBoard(board)
    }

    fun getPieceView(location: BoardIndex, piece: PieceState): PieceView {
        return PieceView(rootView.context, location, piece, rotateBlackPieces)
    }

    fun getScale(): Float {
        return getViewSize().toFloat() / Board.BOARD_SIZE
    }

    fun getViewSize(): Int{
        return rootView.width
    }

    interface FieldClickListener {

        fun onFieldTouchBegin(board: Board, location: BoardIndex)

        fun onFieldRelease(board: Board, location: BoardIndex)

    }

}