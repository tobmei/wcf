package de.tob.wcf

import android.content.Context
import android.graphics.*
import android.util.AttributeSet
import android.util.Log
import android.view.View
import androidx.annotation.ColorInt
import androidx.core.graphics.scale
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.findViewTreeLifecycleOwner
import androidx.lifecycle.findViewTreeViewModelStoreOwner
import androidx.lifecycle.lifecycleScope
import de.tob.wcf.ui.main.*
import kotlinx.coroutines.*
import kotlinx.coroutines.Dispatchers.Default
import kotlinx.coroutines.flow.collectLatest
import java.util.*
import kotlin.collections.HashMap
import kotlin.math.absoluteValue
import kotlin.streams.toList

class GridView @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    private val nCol = 56
    private val nRow = 56
    private var cellWidth: Float = 0f
    private var cellHeight: Float = 0f
    private var screenWidth = 0
    private var screenHeight = 0
    private val entropy = Array(nCol*nRow) {-1}
    private var outputCoords = mutableListOf <Pair<Float,Float>>()
    private lateinit var matrixAdj: HashMap<Int, MutableList<Int>>

    private var bitmap: Bitmap? = null

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
        scope = CoroutineScope(Default)
        scope!!.launch {
            viewModel.eventFlow.collect { event ->
                Log.i(this.javaClass.name, "event collected: $event")
                when (event) {
                    is OutputViewEvent.Start -> {
                        delay(1000)
                        bitmap = Bitmap.createBitmap(nCol, nRow, Bitmap.Config.ARGB_8888)
                        patternList = event.list
                        patternToSum = event.sum
                        patternToAdj = event.adj
                        generateWave()
                        collapse()
                    }
                    is OutputViewEvent.Redo -> {
                        bitmap = Bitmap.createBitmap(nCol, nRow, Bitmap.Config.ARGB_8888)
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
        Log.i(this.javaClass.name, "screen width: $w")
        Log.i(this.javaClass.name, "screen height: $h")
        screenWidth = w
        screenHeight = h
    }

    override fun onDraw(canvas: Canvas?) {
        super.onDraw(canvas)
        wave.forEachIndexed { index, bitSet ->
            if(bitSet.cardinality() == 1) {
                bitSet.stream().forEach {
                    bitmap?.setPixel(index%nCol, (index/nCol).absoluteValue, patternList[it].first())
                    //drawCell(canvas, outputCoords.get(index), patternList.get(it).first())
                }
            }
            if(bitSet.cardinality() == 0) {
                //drawCell(canvas, outputCoords.get(index), getRandomColor())
            }
        }
        bitmap?.let {
            canvas?.drawBitmap(it.scale(screenWidth, screenHeight, false), 0F, 0F, null)
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
            wave[currentCell].stream()?.toList()
                ?.let {
                    getRandomPatternByWeight(it)
                }?.let {
                    wave[currentCell].clear()
                    wave[currentCell].flip(it)
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

    private fun getAdjacentBitSet(adjIndex: Int, current: BitSet): BitSet {
        val adjBitSet = BitSet(patternList.size)
        current.stream()?.forEach { validIndex ->
            val bitSet = patternToAdj[validIndex]?.get(adjIndex)
            adjBitSet.or(bitSet)
        }
        return adjBitSet
    }

    private fun ripple(position: Int) {
        val visited = BitSet(wave.size)  // could be smaller -> size of uncertain waves(+1)
        val carriesChange = BitSet(wave.size)
        val queue: MutableList<Int> = mutableListOf(position)
        carriesChange[position] = true
        var rippleCount = 0
        var depth = 0
        var changeCount = 0

        while (queue.isNotEmpty()) {// && carriesChange.cardinality() > 0) {
            val currWaveIndex = queue.removeAt(0)
            if (!visited[currWaveIndex]) {
                matrixAdj[currWaveIndex]?.forEachIndexed { direction, adjIndex ->
                    if (adjIndex != -1) {
//                        if (!carriesChange[currWaveIndex]) {
//                            queue.add(adjIndex)
//                        } else {
                            if (wave[adjIndex].cardinality() > 1) {
                                val oldWave = wave[adjIndex].clone()
                                rippleCount++
                                wave[adjIndex].and(getAdjacentBitSet(direction, wave[currWaveIndex]))
                                val changed = wave[adjIndex] != oldWave
                                carriesChange[adjIndex] = changed
                                if (changed) changeCount++

                                queue.add(adjIndex)
                            }
                        //}
                    }
                }
                depth++
                if (depth > 300) break
                visited[currWaveIndex] = true
                carriesChange[currWaveIndex] = false
            }
            //if(!carriesChange.any { true }) break
        }
        //Log.i(this.javaClass.name, "rippled waves: $rippleCount")
        //Log.i(this.javaClass.name, "rippled depth: $depth")
        //Log.i(this.javaClass.name, "changed: $changeCount")
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
        matrixAdj = matrixAdj()
    }

    private fun matrixAdj(): HashMap<Int,MutableList<Int>> {
        val adj: HashMap<Int,MutableList<Int>> = hashMapOf()
        (0 until nCol*nRow).forEach { index ->
            val currentRow = (index / nCol).absoluteValue
            val currentCol = index % nCol
            val top = if (currentRow == 0) -1 else index-nCol
            val right = if (currentCol == nCol-1) -1 else index+1
            val bottom = if (currentRow == nRow-1) -1 else index+nCol
            val left = if (currentCol == 0) -1 else index-1
            adj[index] = mutableListOf(top, right, bottom, left)
        }
        return adj
    }

}