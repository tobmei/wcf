package de.tob.wcf

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.util.AttributeSet
import android.util.Log
import android.view.View
import androidx.annotation.ColorInt
import de.tob.wcf.db.Input
import kotlin.math.absoluteValue

class Preview @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    private var input: Input? = null

    private var cellWidth = 0F
    private var cellHeight = 0F
    private var nRow = 0
    private var nCol = 0

    fun setInput(input: Input) {
        this.input = input
        this.nRow = input.y
        this.nCol = input.x
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        cellWidth = w.toFloat() / nCol.toFloat()
        cellHeight = cellWidth
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        input?.pixels?.forEachIndexed { index, pixel ->
            val row = index % nRow
            val col = (index / nCol).absoluteValue
            val x = col * cellWidth
            val y = row * cellHeight
            drawCell(canvas, x, y, pixel)
        }
    }

    private fun drawCell(canvas: Canvas?, x: Float, y: Float, @ColorInt color: Int) {
        val fillPaint = Paint()
        fillPaint.style = Paint.Style.FILL
        fillPaint.color = color
        val strokePaint = Paint()
        strokePaint.style = Paint.Style.STROKE
        strokePaint.strokeWidth = 1F
        strokePaint.color = Color.DKGRAY
        val bottom = y + cellHeight
        val right = x + cellWidth
        canvas?.drawRect(RectF(y, x, bottom, right), fillPaint)
        canvas?.drawRect(RectF(y, x, bottom, right), strokePaint)
    }
}