package de.tob.wcf

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.util.AttributeSet
import android.util.Log
import android.view.GestureDetector
import android.view.MotionEvent
import android.view.View
import androidx.annotation.ColorInt
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.findViewTreeViewModelStoreOwner
import de.tob.wcf.db.Input
import de.tob.wcf.ui.main.*
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.collectLatest
import kotlin.coroutines.coroutineContext
import kotlin.math.absoluteValue

class DrawingView @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    private val viewModel by lazy {
        ViewModelProvider(findViewTreeViewModelStoreOwner()!!)
            .get(DrawingViewModel::class.java)
    }

    private var scope: CoroutineScope? = null

    private lateinit var input: Input
    private var pixels: Array<Int>? = null
    private var currentColor = Color.DKGRAY

    private var cellWidth = 0F
    private var cellHeight = 0F
    private var nRow = 0
    private var nCol = 0

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)

        scope!!.launch {
            viewModel.pixelFlow.collectLatest { pixelState ->
                when (pixelState) {
                    is PixelState.Pixels -> {
                        input = pixelState.input
                        pixels = pixelState.input.pixels.toTypedArray()
                        nCol = Math.sqrt(pixels!!.size.toDouble()).toInt()
                        nRow = nCol
                        invalidate()
                    }
                }
            }
        }.cancel()

        scope!!.launch {
            viewModel.eventFlow.collect { event ->
                when (event) {
                    is DrawingViewEvent.ColorChanged -> {
                        currentColor = event.color
                    }
                    is DrawingViewEvent.CanvasSetup -> {
                        nCol = event.nCol
                        nRow = event.nRow
                    }
                }
            }
        }
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        scope?.cancel()
        scope = null
    }

    @SuppressLint("ClickableViewAccessibility")
    override fun onTouchEvent(event: MotionEvent?): Boolean {
        if (event != null) {
            when (event.action) {
                MotionEvent.ACTION_MOVE -> {
                    val idx = coordToIndex(event.x,event.y)
                    pixels?.set(idx, currentColor)
                    invalidate()
                }
            }
            viewModel.mutatePixelState { PixelState.Pixels(input.copy(pixels=pixels!!.toList())) }
            return true
        }
        return false
    }

    private fun coordToIndex(x: Float, y: Float): Int {
        val col = (x / cellWidth).toInt()
        val row = (y / cellHeight).toInt()
        val index = row * nCol + col
        return index
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        Log.i(this.javaClass.name, "onSizeChanged width: $w")
        Log.i(this.javaClass.name, "onSizeChanged height: $h")
        cellWidth = w.toFloat() / nCol.toFloat()
        cellHeight = cellWidth
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        pixels?.forEachIndexed { index, pixel ->
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
        strokePaint.strokeWidth = 2F
        strokePaint.color = Color.DKGRAY
        val bottom = y + cellHeight
        val right = x + cellWidth
        canvas?.drawRect(RectF(y, x, bottom, right), fillPaint)
        canvas?.drawRect(RectF(y, x, bottom, right), strokePaint)
    }
}