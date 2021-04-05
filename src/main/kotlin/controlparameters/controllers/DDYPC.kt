package controlparameters.controllers

import controlparameters.Parameterized
import util.Selection
import util.random.RandomProvider
import kotlin.math.max

/**
 * A generic parameter controller based on the concept proposed by Doerr, Doerr and Yang.
 *
 * Source: Doerr, Benjamin, Carola Doerr, and Jing Yang. "k-Bit mutation with self-adjusting k outperforms standard bit mutation." International Conference on Parallel Problem Solving from Nature. Springer, Cham, 2016.
 *
 * @property delta Learning rate.
 * @property eps Forgetting rate.
 * @property ranges Number of ranges per parameter.
 * @property exploringGenerationsPerRange Number of initial epochs where exploring is forced.
 * @property greedy Select the best range for a parameter greedily? Applied for exploitation only. (Otherwise, the ranges are ranked by their respective utilities and then selected rank-based.)
 * @constructor Creates a new instance of DDYPC.
 *
 * @param parameterized The instance of the Parmaterized-objective, i.e. E(MO)A, to control.
 * @param random A random provider.
 */
class DDYPC(
    parameterized: Parameterized,
    random: RandomProvider,
    val delta: Double = 0.2,
    val eps: Double = 0.01,
    val ranges: Int = 10,
    val exploringGenerationsPerRange: Int = 10,
    val greedy: Boolean = true
) : ParameterController(parameterized, random) {

    override val requiresUpdate = true

    /**
     * Number of generations that are forced to be exploring generations at the beginning of the evolutionary process.
     */
    private val exploringGenerations = ranges * exploringGenerationsPerRange

    /**
     * Determines whether DDYPC is currently in exploration or exploitation mode.
     */
    private var exploration = true

    /**
     * Index of the best known range for each parameter.
     */
    private val bestRange = Array(parameters.size) { -1 }

    /**
     * Stores the utilities for each parameter and range.
     */
    val util = Array(parameters.size) { Array(ranges) { ParameterUtil() } }

    /**
     * Stores the indices of the selected parameter ranges (in the current generation).
     */
    private var selected = Array(parameters.size) { 0 }

    /**
     * Stores the quality value of the previous population (w.r.t. the generation).
     */
    private var qualityPrevious = 0.0

    /**
     * Counts the number of update calls.
     */
    private var t = 0


    /**
     * Returns the best range for the parameter at the given index. If multiple ranges have equal best utility, a random index among those is returned.
     *
     * @param parameterIndex The parameter's index.
     * @return The index of the (or a) best interval.
     */
    private fun getBestRange(parameterIndex: Int): Int {
        val utilRanges = util[parameterIndex]

        if (greedy) {
            val utilMax = utilRanges.maxByOrNull { it.v }!!.v

            val bestIndices = mutableListOf<Int>()

            for (rangeIndex in utilRanges.indices) {
                val utility = utilRanges[rangeIndex].v

                if (utility == utilMax) {
                    bestIndices.add(rangeIndex)
                }
            }

            if (bestRange[parameterIndex] == -1 || !bestIndices.contains(bestRange[parameterIndex])) {
                bestRange[parameterIndex] = bestIndices.random()
            }
        } else {
            data class ValueIndex(val value: Double, val index: Int)

            val lst = utilRanges.mapIndexed { i, v -> ValueIndex(v.v, i) }.toMutableList()
            lst.sortBy { it.value }

            val ld = Selection.linearDistribution(utilRanges.size, 0.99, false)
            val distribution = Array(utilRanges.size) { 0.0 }

            for (i in 0 until lst.size) {
                distribution[lst[i].index] = ld[i]
            }

            bestRange[parameterIndex] = Selection.rouletteWheel(distribution, random!!)
        }

        return bestRange[parameterIndex]
    }


    override fun set() {
        exploration = t < exploringGenerations || random!!.nextDouble() <= delta

        parameters.forEachIndexed { index, parameter ->

            // Select an interval and determine [min, max].
            val rangeIndex = if (exploration) random!!.nextInt(ranges) else getBestRange(index)

            val min = rangeIndex.toDouble() / ranges.toDouble()
            val max = (rangeIndex + 1).toDouble() / ranges.toDouble()

            // Save the currently selected value.
            selected[index] = rangeIndex


            // Set the value to the parameterized object.
            parameterized.set(parameter, min + random!!.nextDouble() * (max - min))
        }
    }

    override fun update(quality: Double) {
        // Determine the fitness difference.
        val qualityDiff = if (neverUpdatedYet) 0.0 else max(0.0, quality - qualityPrevious)

        qualityPrevious = quality

        // Update the velocities and weights for all parameters and intervals.
        for (index in selected.indices) {
            for (r in 0 until ranges) {
                val pv = util[index][r]

                val pvwNext = (1.0 - eps) * pv.w

                // Determine whether to update only weight or also velocity (in case the according range r has been selected for the current epoch.
                if (selected[index] == r) {
                    pv.w = pvwNext + 1.0
                    pv.v = (pvwNext * pv.v + qualityDiff) / pv.w
                } else {
                    pv.w = pvwNext
                }
            }
        }

        neverUpdatedYet = false
        t++
    }


    /**
     * A parameter utility and weight storing class.
     *
     * @property v Velocity = Utility.
     * @property w Weight.
     * @constructor Creates an instance of ParameterUtil.
     */
    data class ParameterUtil(var v: Double = 0.0, var w: Double = 0.0)
}