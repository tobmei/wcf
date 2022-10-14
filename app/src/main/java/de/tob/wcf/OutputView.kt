package de.tob.wcf

import android.content.Context
import android.graphics.*
import android.util.AttributeSet
import android.util.Log
import android.view.View
import androidx.annotation.ColorInt
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.findViewTreeLifecycleOwner
import androidx.lifecycle.findViewTreeViewModelStoreOwner
import androidx.lifecycle.lifecycleScope
import de.tob.wcf.ui.main.*
import kotlinx.coroutines.*
import kotlinx.coroutines.Dispatchers.Default
import kotlinx.coroutines.flow.collectLatest
import java.util.*
import kotlin.streams.toList

class GridView @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    private val nCol = 48
    private val nRow = 48
    private var cellWidth: Float = 0f
    private var cellHeight: Float = 0f
    private var screenWidth = 0
    private var screenHeight = 0
    private val entropy = Array(nCol*nRow) {-1}
    private var outputCoords = mutableListOf <Pair<Float,Float>>()

    private lateinit var patternList: List<List<Int>>
    private lateinit var patternToSum: Map<Int, Int>
    private lateinit var patternToAdj: Map<Int, MutableList<BitSet>>

    private var wave = arrayOf<BitSet>()

    private val random = Random()

    private val viewModel by lazy {
        ViewModelProvider(findViewTreeViewModelStoreOwner()!!)
            .get(OutputViewModel::class.java)
    }

    private var scope: CoroutineScope? = null

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        scope = findViewTreeLifecycleOwner()?.lifecycleScope
        scope!!.launch(Default) {
            viewModel.eventFlow.collect { event ->
                Log.i(this.javaClass.name, "event collected: $event")
                when (event) {
                    is OutputViewEvent.Start -> {
                        delay(1000)
                        patternList = event.list
                        patternToSum = event.sum
                        patternToAdj = event.adj
                        generateWave()
                        collapse()
                    }
                    is OutputViewEvent.Redo -> {
                        generateWave()
                        collapse()
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

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        setupOutputDimensions(w,h)
    }

    override fun onDraw(canvas: Canvas?) {
        super.onDraw(canvas)
        wave.forEachIndexed { index, bitSet ->
            val coord = outputCoords.get(index)
            if(bitSet.cardinality() == 1) {
                bitSet.stream().forEach {
                    drawCell(canvas, coord, patternList.get(it).first())
                }
            }
            if(bitSet.cardinality() == 0) {
                drawCell(canvas, coord, getRandomColor())
            }
        }
    }

    private fun collapse() {
        var iteration = 0
        while (!isFinished()) {
            if(hasZeroWave()) {
                println("zero wave")
                wave.forEach { bits ->
                    if (bits.cardinality() == 0) {
                        bits.set(0, wave.size-1)
                    }
                }
                //break
            }

            //get uncertain tile with lowest amount of possible patterns
            var index = (0..wave.size).random()
            if(iteration > 0) {
               index = getCellWithLowestEntropy()
            }
            iteration++

            val currentCell = index
            if (currentCell == -1) return

            //pick random possible pattern to draw in tile
            //val patternToDraw = wave?.get(currentCell)?.stream()?.toArray()?.random()
            val patternToDraw = wave.get(currentCell).stream()?.toList()
                ?.let {
                    getRandomPatternByWeight(it)
                }?.let {
                    wave.get(currentCell).clear()
                    wave.get(currentCell).flip(it)
                }
            entropy[currentCell] = 1
            invalidate()

            //propagate changes to other tiles
            ripple(currentCell)
        }
        Log.d("-----", "Finshed")
    }

    private fun isFinished() = wave.sumOf { it.cardinality() } == nCol*nRow

    private fun hasZeroWave() : Boolean {
        wave.forEach {
            if (it.cardinality() == 0) return true
        }
        return false
    }

    private fun getRandomPatternByWeight(patterns: List<Int>) : Int? {
        val candidates = patternToSum.mapKeys { entry -> entry.key.takeIf { patterns.contains(entry.key) } }
            .toList().sortedBy { (_, v) -> v }.toMap()
        val highest = candidates.values.last()
        val rand = (1..highest).random()
        candidates.values.forEach { v ->
            if (v >= rand) {
                return candidates.filterValues { it == v }.keys.random()
            }
        }
        return null
    }

    private fun updateEntropy() =
        wave.forEachIndexed { index, bitSet ->
            entropy[index] = bitSet.cardinality()
        }

    private fun ripple(position: Int) {
        val rippledCells = mutableListOf<Int>()
        val fixedCells = mutableListOf<Int>()
        var currentPosition = position

        while (true) {
            val currentWave = wave[currentPosition]
            val outY = outputCoords[currentPosition].first
            val outX = outputCoords[currentPosition].second
            val rightWaveIndex = outputCoords.indexOf(Pair(outY,outX+cellWidth))
            val leftWaveIndex = outputCoords.indexOf(Pair(outY,outX-cellWidth))
            val topWaveIndex = outputCoords.indexOf(Pair(outY-cellHeight,outX))
            val bottomWaveIndex = outputCoords.indexOf(Pair(outY+cellHeight,outX))

            val validIds = currentWave.stream()?.toArray()
            if (validIds?.isEmpty() == true) {
                return
            }

            if (!fixedCells.contains(rightWaveIndex) && rightWaveIndex != -1 && wave[rightWaveIndex].cardinality() > 1) {
                val oldWave = wave[rightWaveIndex].clone()
                val adjBitSet = BitSet(nCol*nRow)
                currentWave.stream()?.forEach { validIndex ->
                    val bitSet = patternToAdj.get(validIndex)?.get(1)
                    adjBitSet.or(bitSet)
                }
                wave[rightWaveIndex].and(adjBitSet)
                if (oldWave != wave[rightWaveIndex]) rippledCells.add(rightWaveIndex)
                entropy[rightWaveIndex] = wave[rightWaveIndex].cardinality()
            }

            if (!fixedCells.contains(leftWaveIndex) && leftWaveIndex != -1 && wave[leftWaveIndex].cardinality() > 1) {
                val oldWave = wave[leftWaveIndex].clone()
                val adjBitSet = BitSet(nCol*nRow)
                currentWave.stream()?.forEach { validIndex ->
                    val bitSet = patternToAdj[validIndex]?.get(3)
                    adjBitSet.or(bitSet)
                }
                wave[leftWaveIndex].and(adjBitSet)
                if (oldWave != wave[leftWaveIndex]) rippledCells.add(leftWaveIndex)
                entropy[leftWaveIndex] = wave[leftWaveIndex].cardinality()

            }

            if (!fixedCells.contains(topWaveIndex) && topWaveIndex != -1 && wave[topWaveIndex].cardinality() > 1) {
                val oldWave = wave[topWaveIndex].clone()
                val adjBitSet = BitSet(nCol*nRow)
                currentWave.stream()?.forEach { validIndex ->
                    val bitSet = patternToAdj[validIndex]?.get(0)
                    adjBitSet.or(bitSet)
                }
                wave[topWaveIndex].and(adjBitSet)
                if (oldWave != wave[topWaveIndex]) rippledCells.add(topWaveIndex)
                entropy[topWaveIndex] = wave[topWaveIndex].cardinality()

            }

            if (!fixedCells.contains(bottomWaveIndex) && bottomWaveIndex != -1 && wave[bottomWaveIndex].cardinality() > 1) {
                val oldWave = wave[bottomWaveIndex].clone()
                val adjBitSet = BitSet(nCol*nRow)
                currentWave.stream()?.forEach { validIndex ->
                    val bitSet = patternToAdj[validIndex]?.get(2)
                    adjBitSet.or(bitSet)
                }
                wave[bottomWaveIndex].and(adjBitSet)
                if (oldWave != wave[bottomWaveIndex]) rippledCells.add(bottomWaveIndex)
                entropy[bottomWaveIndex] = wave[bottomWaveIndex].cardinality()

            }

            fixedCells.add(currentPosition)
            rippledCells.remove(currentPosition)
            if (rippledCells.isEmpty()) return
            val lowestEntropy = rippledCells.indexOf(rippledCells.minBy { entropy[it] })
            currentPosition = rippledCells[lowestEntropy]
            //currentPosition = rippledCells.min()
        }
    }

    private fun getCellWithLowestEntropy() : Int {
        val map = mutableMapOf<Int,MutableList<Int>>()
        wave.forEachIndexed{ i, w ->
            map.putIfAbsent(w.cardinality(), mutableListOf(i))?.add(i)
        }
        val selection =  map.toSortedMap().firstNotNullOf { pair -> pair.value.takeIf { pair.key > 1 } }
        return selection.get(random.nextInt(selection.size))
    }

    private fun generateWave() {
        wave = Array(nCol*nRow) { BitSet(patternList.size) }
        wave.forEach {
            it.flip(0,patternList.size)
        }
    }

    private fun setupOutputDimensions(w: Int, h: Int) {
        screenWidth = w
        screenHeight = h
        cellWidth = w.toFloat() / nCol.toFloat()
        cellHeight = cellWidth
        var x = 0f
        for (i in 0 until nRow) {
            var y = 0f
            for (j in 0 until nCol){
                outputCoords.add(Pair(x,y))
                y += cellWidth
            }
            x += cellHeight
        }
    }

    private fun drawCell(canvas: Canvas?, coord: Pair<Float,Float>, color: Int) {
        val top = coord.first
        val left = coord.second
        val bottom = coord.first + cellHeight
        val right = coord.second + cellWidth
        canvas?.drawRect(
            RectF(top, left, bottom, right),
            getPaint(color)
            //getPaint(getRandomColor())
        )
    }

    private fun getPaint(@ColorInt color: Int) : Paint{
        val paint = Paint()
        paint.blendMode = BlendMode.MULTIPLY
        paint.color = color
        return paint
    }

    fun getRandomColor(): Int {
        val rnd = Random()
        return Color.argb(255, rnd.nextInt(256), rnd.nextInt(256), rnd.nextInt(256))
    }

}