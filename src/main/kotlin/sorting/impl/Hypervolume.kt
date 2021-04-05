package sorting.impl


import org.apache.commons.math3.util.FastMath
import sorting.ParetoDominance

import sorting.QProcedure
import sorting.ReferenceBasedQP

/**
 * Hypervolume (HSO). Based on the implementation by While et al.
 *
 * SOURCE: While, Lyndon, et al. "A faster algorithm for calculating hypervolume." IEEE transactions on evolutionary computation 10.1 (2006): 29-38.
 *
 * @constructor Creates a new instance.
 *
 * @param iterative If true, the q-Procedure runs in iterative mode.
 * @param next The subordinate q-Procedure. Not mandatory.
 */
class Hypervolume(iterative: Boolean, next: QProcedure?) : ReferenceBasedQP(iterative, next) {

    companion object {
        val bkMap = hashMapOf<Int, Array<Int>>()

        fun fillBKMap(k: Int, iterations: Int) {
            val arr = Array(iterations) { 0 }
            for (H in 1..iterations) {
                arr[H - 1] = bk(H + k, k)
            }

            bkMap[k] = arr
        }

        fun bk(n: Int, k_: Int): Int {
            val k = if (2 * k_ > n) n - k_ else k_

            var res = 1
            for (i in 1..k) {
                res = (res * (n - k + i)) / i
            }

            return res
        }
    }

    constructor(iterative: Boolean) : this(iterative, null)
    constructor() : this(false, null)


    override val key: Int
        get() = 3
    override val name: String
        get() = "Hypervolume"
    override val nameShort: String
        get() = "HV"
    override val normalizeDuringEvolution: Boolean
        get() = true

    override fun computeValue(matrix: Array<Array<Double>>): Double {
        if (updateEveryCall) {
            // Update the reference point.

            if (isEvolutionMode) {
                updateReferencePoint(Array(matrix[0].size) { 1.0 }, matrix.size)
            } else {
                updateReferencePoint(matrix[0].size, 1.1)
            }
        }

        return hso(mutableListOf(*matrix)) / referencePointCorrectionFactor
    }

    override fun computeContribution(matrix: Array<Array<Double>>): Array<Double> {
        val all = computeValue(matrix)

        val contrib = Array(matrix.size) { 0.0 }

        // Apply the set-subset approach.
        val invertedFrontSubset = Array(matrix.size - 1) { Array(matrix[0].size) { 0.0 } }

        for (index in matrix.indices) {
            System.arraycopy(matrix, 0, invertedFrontSubset, 0, index)
            System.arraycopy(matrix, index + 1, invertedFrontSubset, index, invertedFrontSubset.size - index)

            val hv = computeValue(invertedFrontSubset)
            contrib[index] = all - hv
        }

        return contrib
    }


    /**
     * Updates reference point. Based on the approach of Ishibuchi et al.
     *
     * SOURCE: Ishibuchi, Hisao, et al. "How to specify a reference point in hypervolume calculation for fair performance comparison." Evolutionary computation 26.3 (2018): 411-440.
     *
     * @param point
     * @param setSize
     */
    override fun updateReferencePoint(point: Array<Double>, setSize: Int) {

        if (!bkMap.containsKey(point.size - 1)) {
            fillBKMap(point.size - 1, 62)
        }

        var H = -1
        val hValues = bkMap[point.size - 1]!!

        for (h in 0 until hValues.size - 1) {
            if (hValues[h] <= setSize && setSize < hValues[h + 1]) {
                H = 1 + h
                break
            }
        }

        if (H == -1) {
            H = point.size
        }

        val r = 1.0 + (1.0 / H.toDouble())

        referencePointCorrectionFactor = 1.0

        for (k in point.indices) {
            point[k] *= r

            referencePointCorrectionFactor *= r
        }

        referencePoint = point
    }

    /**
     * HSO main procedure.
     *
     * @param pl Object vector matrix.
     * @return Hypervolume dominated by the objective vectors.
     */
    fun hso(pl: MutableList<Array<Double>>): Double {
        val K = pl[0].size

        pl.sortBy { x -> x[0] }

        var slices = mutableListOf<Pair<Double, List<Array<Double>>>>()

        slices.add(Pair(1.0, pl))

        for (k in 0 until K - 2) {
            val newSlices = mutableListOf<Pair<Double, List<Array<Double>>>>()

            for (slice in slices) {
                val ql = slice.second.toList()
                newSlices.addAll(slice(ql, k, slice.first))
            }

            slices = newSlices
        }

        // All slices are 2d now.
        var vol = 0.0

        for (slice in slices) {
            vol += hv2D(slice.second, slice.first)
        }

        return vol
    }

    /**
     * Hypervolume in the 2-dimensional case.
     */
    fun hv2D(front: List<Array<Double>>, depth: Double): Double {
        val K = front[0].size

        var vol = 0.0

        for (i in 0 until front.size - 1) {
            vol += (front[i + 1][K - 2] - front[i][K - 2]) * (referencePoint[K - 1] - front[i][K - 1])
        }

        vol += (referencePoint[K - 2] - front.last()[K - 2]) * (referencePoint[K - 1] - front.last()[K - 1])

        return vol * depth
    }

    /**
     * HSO specific.
     */
    fun slice(pl: List<Array<Double>>, k: Int, parentDepth: Double): List<Pair<Double, List<Array<Double>>>> {
        val slices = mutableListOf<Pair<Double, List<Array<Double>>>>()

        var ql = listOf<Array<Double>>()

        var p = pl[0]

        for (i in 1 until pl.size) {
            ql = insert(p, k + 1, ql)

            val p_ = pl[i]

            val depth = FastMath.abs(p[k] - p_[k])

            if (depth > 0) {
                slices.add(Pair(parentDepth * depth, ql))
            }

            p = p_
        }

        ql = insert(p, k + 1, ql)

        val depth = FastMath.abs(p[k] - referencePoint[k])

        if (depth > 0) {
            slices.add(Pair(parentDepth * depth, ql))
        }

        return slices
    }

    /**
     * HSO specific.
     */
    fun insert(p: Array<Double>, k: Int, unsorted: List<Array<Double>>): List<Array<Double>> {
        val sortedByK = mutableListOf<Array<Double>>()

        var index = -1

        for (i in 0 until unsorted.size) {
            val unsrt = unsorted[i]

            if (unsrt[k] < p[k]) {
                sortedByK.add(unsrt)
            } else {
                index = i
                break
            }
        }

        sortedByK.add(p)

        if (index != -1) {
            for (i in index until unsorted.size) {
                val unsrt = unsorted[i]

                if (ParetoDominance.dominanceTest(p, unsrt, k) != -1) {
                    sortedByK.add(unsrt)
                }
            }
        }

        return sortedByK
    }
}