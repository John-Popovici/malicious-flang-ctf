package de.tadris.flang.ui.board

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Path
import android.view.View
import de.tadris.flang_lib.Move
import de.tadris.flang_lib.getFromIndex
import de.tadris.flang_lib.getToIndex
import de.tadris.flang_lib.x
import de.tadris.flang_lib.y
import kotlin.math.*

class ArrowFieldView(context: Context?, private val move: Move, private val boardView: BoardView, color: Int) : View(context), FieldView {

    private val scale = boardView.getScale()
    private val width = scale / 4

    private val posFromX: Float
    private val posFromY: Float
    private val posToX: Float
    private val posToY: Float

    init {
        val indexFrom = boardView.project(move.getFromIndex())
        posFromX = indexFrom.x * scale + (scale/2)
        posFromY = indexFrom.y * scale + (scale/2)
        val indexTo = boardView.project(move.getToIndex())
        posToX = indexTo.x * scale + (scale/2)
        posToY = indexTo.y * scale + (scale/2)
    }

    private val backgroundPaint = Paint()
    private val foregroundPaint = Paint()

    init {
        backgroundPaint.color = 0x00ffffff

        foregroundPaint.style = Paint.Style.STROKE
        foregroundPaint.strokeWidth = width
        foregroundPaint.color = color
    }

    override fun onDraw(canvas: Canvas) {
        //Paint Background
        canvas.drawRect(0f, 0f, width, height.toFloat(), backgroundPaint)

        //Draw Shaft
        val xDiff = posToX - posFromX
        val yDiff = posToY - posFromY
        val length = sqrt(xDiff.pow(2) + yDiff.pow(2))

        val xDiffNormal = xDiff / length
        val yDiffNormal = yDiff / length

        val xCutout = xDiffNormal * (width * 3 / 2)
        val yCutout = yDiffNormal * (width * 3 / 2)

        canvas.drawLine(posFromX, posFromY, posToX - xCutout, posToY - yCutout, foregroundPaint)

        val path = Path()
        path.fillType = Path.FillType.EVEN_ODD

        path.moveTo(posToX, posToY)
        path.lineTo(posToX + width, posToY + (width * 2 / 3))
        path.lineTo(posToX + width, posToY - (width * 2 / 3))
        path.close()

        // Calculate the angle using atan2 for correct quadrant handling
        val angle = atan2(yDiff, xDiff) * 180 / PI.toFloat() + 180

        canvas.rotate(angle, posToX, posToY)
        canvas.drawPath(path, foregroundPaint)
    }

    override fun isFullOverlay() = true

    override fun getLocation() = move.getToIndex()

    override fun getView() = this
}