package de.tob.wcf

import android.content.Context
import android.graphics.*
import android.os.Build
import android.util.AttributeSet
import android.util.Log
import android.view.View
import androidx.annotation.ColorInt
import androidx.annotation.RequiresApi
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers.Default
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.util.*
import java.util.stream.Collectors.toList
import kotlin.streams.toList

private const val N = 3
private const val INPUT = R.drawable.test15

class GridView @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    private val nCol = 32
    private val nRow = 32

    private var cellWidth: Float = 0f
    private var cellHeight: Float = 0f
    private var outputCoords = mutableListOf<Pair<Float,Float>>()

    private var screenWidth = 0
    private var screenHeight = 0

    private val inputX = 24
    private val inputY = 24

    private var patternList = mutableListOf<List<Int>>()
    private var patternToSum = mutableMapOf<Int, Int>()
    private var patternToAdj = mutableMapOf<Int, MutableList<BitSet>>()

    private val bitmapInput = BitmapFactory.decodeResource(resources, INPUT)

    private var wave = arrayOf<BitSet>()
    private val entropy = Array(nCol*nRow) {-1}

    private val random = Random()

    fun start() {
        invalidate()
        generateWave()

        CoroutineScope(Default).launch {
            delay(2000)
            collapse()
        }
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        setupOutputDimensions(w,h)
        getPatternsFromBitmap()
        generateWave()
        invalidate()
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

    private suspend fun collapse() {
        var iteration = 0
        while (!isFinished()) {
            if(hasZeroWave()) {
                println("zero wave")
                break
            }

            //get uncertain tile with lowest amount of possible patterns
            var index = 0
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

            //set other patterns to false for current tile

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

    private fun getPatternsFromBitmap() {
        val patterns = mutableListOf<List<Int>>()
        for (x in 0 until inputX - N+1) {
            for (y in 0 until inputY - N+1) {
                //pattern starts here
                val pattern = mutableListOf<Int>()
                for (i in 0 until N) {
                    for (j in 0 until N) {
                        pattern.add(bitmapInput.getPixel(x + i, y + j))
                    }
                }
                val r90 = rotatePattern(pattern)
                val r180 = rotatePattern(r90)
                val r270 = rotatePattern(r180)
                val hor = swapHorizontal(pattern)
                val vert = swapVertical(pattern)
                listOf(pattern, r90, r180, r270, hor, vert).forEach {
                    if (!patternList.contains(it)) patternList.add(it)

                    patternToSum.putIfAbsent(patternList.indexOf(it), 1)?.let { sum ->
                        patternToSum[patternList.indexOf(it)] = sum+1
                    }

                    patternToAdj.putIfAbsent(patternList.indexOf(it), mutableListOf(
                        BitSet(patternList.size), BitSet(patternList.size),
                        BitSet(patternList.size), BitSet(patternList.size)))
                }
            }
        }
        patternToAdj.keys.forEach { patternId ->
            val currentPattern = patternList.get(patternId)
            patternList.forEachIndexed { index, pattern ->
                if (topSide(currentPattern) == bottomSide(pattern) )
                    patternToAdj[patternId]!!.get(0).flip(patternList.indexOf(pattern))
                if (rightSide(currentPattern) == leftSide(pattern) )
                    patternToAdj[patternId]!!.get(1).flip(patternList.indexOf(pattern))
                if (bottomSide(currentPattern) == topSide(pattern) )
                    patternToAdj[patternId]!!.get(2).flip(patternList.indexOf(pattern))
                if (leftSide(currentPattern) == rightSide(pattern) )
                    patternToAdj[patternId]!!.get(3).flip(patternList.indexOf(pattern))
            }
        }

    }

    private fun rightSide(pattern: List<Int>) = pattern.slice(3..8)
    private fun leftSide(pattern: List<Int>) = pattern.slice(0..5)
    private fun topSide(pattern: List<Int>) = pattern.slice(listOf(0,3,6,1,4,7))
    private fun bottomSide(pattern: List<Int>) = pattern.slice(listOf(1,4,7,2,5,8))

    private fun rotatePattern(pattern: List<Int>) : List<Int> {
        val rotation = MutableList(pattern.size) {0}
        pattern.forEachIndexed { index, i ->
            rotation[(index*3+2)%10] = i
        }
        return rotation
    }

    private fun swapVertical(pattern: List<Int>) : List<Int> {
        val out = pattern.toMutableList()
        out.swap(0,6)
        out.swap(1,7)
        out.swap(2,8)
        return out
    }

    private fun swapHorizontal(pattern: List<Int>) : List<Int> {
        val out = pattern.toMutableList()
        out.swap(0,2)
        out.swap(3,5)
        out.swap(6,8)
        return out
    }

    fun <T> MutableList<T>.swap(index1: Int, index2: Int){
        val tmp = this[index1]
        this[index1] = this[index2]
        this[index2] = tmp
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
        invalidate()
    }

    private fun getPaint(@ColorInt color: Int) : Paint{
        val paint = Paint()
        paint.color = color
        return paint
    }

    fun getRandomColor(): Int {
        val rnd = Random()
        return Color.argb(255, rnd.nextInt(256), rnd.nextInt(256), rnd.nextInt(256))
    }

}