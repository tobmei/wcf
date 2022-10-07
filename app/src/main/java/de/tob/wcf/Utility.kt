package de.tob.wcf

import android.graphics.Bitmap
import androidx.lifecycle.Transformations.map
import de.tob.wcf.db.Input
import java.util.*
import kotlin.math.absoluteValue

object Utility {

    fun getPatternsFromInput(input: Input) : List<Input> {
        val patternList = mutableListOf<List<Int>>()
        val patternToSum = mutableMapOf<Int, Int>()
        val patternToAdj = mutableMapOf<Int, MutableList<BitSet>>()
        val pixels = input.pixels
        println(pixels.distinct())
        val n = 3
        val twoD = mutableMapOf<Int,MutableList<Int>>()
        pixels.forEachIndexed { index, i ->
            twoD.putIfAbsent((index/input.y).absoluteValue, mutableListOf(i))?.add(i)
        }
        val nCol = twoD.size
        val nRow = twoD[0]!!.size

        for (x in 0 until nCol - n+1) {
            for (y in 0 until nRow - n+1) {
                //pattern starts here
                val pattern = mutableListOf<Int>()
                for (i in 0 until n) {
                    for (j in 0 until n) {
                        pattern.add(twoD[x + i]!![y + j])
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
                        BitSet(patternList.size), BitSet(patternList.size)
                    ))
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

        return patternList.map { Input(x=n, y=n, pixels=it) }
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

    private fun <T> MutableList<T>.swap(index1: Int, index2: Int){
        val tmp = this[index1]
        this[index1] = this[index2]
        this[index2] = tmp
    }
}