package controlparameters.controllers

import controlparameters.Parameterized
import util.random.RandomProvider
import kotlin.math.log

/**
 * Entropy-Based Adaptive Range Parameter Control.
 *
 * Source: Aleti, Aldeida, and Irene Moser. "Entropy-based adaptive range parameter control for evolutionary algorithms." Proceedings of the 15th annual conference on Genetic and evolutionary computation. 2013.
 *
 * @property updateAfterNSamples The number of epochs between the update of utilities and ranges.
 * @constructor Creates a new instance of EARPC.
 *
 * @param parameterized The instance of the Parmaterized-objective, i.e. E(MO)A, to control.
 * @param random A random provider.
 */
class EARPC(parameterized: Parameterized, random: RandomProvider, val updateAfterNSamples: Int = 50) :
    ParameterController(parameterized, random) {

    override val requiresUpdate = true

    /**
     * Contains the splitting points for the two ranges for each parameter.
     */
    private val rangeSplit = Array(parameters.size) { 0.5 }

    /**
     * Contains the selection probabilities for the first parameter range for each parameter. The selection probability for the second range equals 1 - p(first range).
     */
    private val probabilitySplit = Array(parameters.size) { 0.5 }

    /**
     * Storage for the previously selected parameter values (and the respective utilities).
     */
    private val history = Array(parameters.size) { mutableListOf<ParameterUtil>() }

    /**
     * Selected parameter values (current generation).
     */
    private var selected = Array(parameters.size) { ParameterUtil(-1.0, -1.0) }

    /**
     * Stores the quality value of the previous population (w.r.t. the generation).
     */
    private var qualityPrevious = 0.0

    override fun set() {
        parameters.forEachIndexed { index, parameter ->

            // Select a value from either interval.
            val v = if (random!!.nextDouble() <= probabilitySplit[index]) {
                random.nextDouble() * rangeSplit[index]
            } else {
                rangeSplit[index] + random.nextDouble() * (1.0 - rangeSplit[index])
            }

            // Save the currently selected value.
            selected[index] = ParameterUtil(v, -1.0)

            // Set the value to the parameterized object.
            parameterized.set(parameter, v)
        }
    }

    override fun update(quality: Double) {
        // Determine the fitness difference.
        val qualityDiff = if (neverUpdatedYet) 0.0 else quality - qualityPrevious

        qualityPrevious = quality

        // Set the utility for all selected values and add to history.
        for (index in selected.indices) {
            selected[index].util = qualityDiff
            history[index].add(selected[index])
        }

        if (history[0].size >= updateAfterNSamples) {
            adjustRange()
        }

        neverUpdatedYet = false
    }

    /**
     * Adjusts the ranges for all parameters. Main procedure of EARPC.
     *
     */
    private fun adjustRange() {
        for (index in parameters.indices) {
            val points = history[index]

            // 1. Sort the samples by their values (ascending).
            points.sortBy { it.value }

            // 2. Normalize the utilities between 0 and 1.
            var uMin = Double.MAX_VALUE
            var uMax = Double.NEGATIVE_INFINITY

            for (point in points) {
                if (point.util > uMax) uMax = point.util
                if (point.util < uMin) uMin = point.util
            }

            if (uMin < uMax) {
                points.forEach { it.util = (it.util - uMin) / (uMax - uMin) }
            }

            // Note: The parameter values are relative, i.e. always within [0, 1].

            // 3. Assign the sample vectors (value, utility) into two clusters (Lloyd algorithm).
            val centroids = Array(2) { random!!.nextInt(points.size - 1) }
            while (centroids[0] == centroids[1]) centroids[1] = random!!.nextInt(points.size - 1)

            while (true) {
                for (point in points) {
                    var diffMin = Double.MAX_VALUE

                    for (clusterIndex in centroids.indices) {
                        val diff = point.distance(points[clusterIndex])

                        if (diff < diffMin) {
                            diffMin = diff
                            point.clusterID = clusterIndex
                        }
                    }
                }

                // Sum the values and utilities of all members of each cluster. Count the number of samples of each cluster.
                val sum = Array(centroids.size) { Array(2) { 0.0 } }
                val count = Array(centroids.size) { 0 }

                for (point in points) {
                    sum[point.clusterID][0] += point.value
                    sum[point.clusterID][1] += point.util
                    count[point.clusterID]++
                }

                // Determine the next cluster center points.
                val mean = Array(centroids.size) { i -> Array(2) { j -> sum[i][j] / count[i].toDouble() } }
                val meanPoints = Array(centroids.size) { i -> ParameterUtil(mean[i][0], mean[i][1], -1) }

                val distanceMin = Array(centroids.size) { Double.MAX_VALUE }
                val indexMin = Array(centroids.size) { -1 }

                for (index_ in points.indices) {
                    val clusterID = points[index_].clusterID
                    val distance = points[index_].distance(meanPoints[clusterID])

                    if (distance < distanceMin[clusterID]) {
                        distanceMin[clusterID] = distance
                        indexMin[clusterID] = index_
                    }
                }

                // Determine whether any of the center points has changed. If not, break the loop.
                var allEqual = true

                for (index_ in centroids.indices) {
                    if (centroids[index_] != indexMin[index_]) allEqual = false

                    centroids[index_] = indexMin[index_]
                }

                if (allEqual) break
            }

            // 4. Find cutting points.
            var entropyMin = Double.MAX_VALUE
            var cutPoint = -1

            // Find the cut-point for which the entropy becomes minimal.
            for (k in 0 until points.size - 1) {
                val matching1 = Array(centroids.size) { 0 }
                val matching2 = Array(centroids.size) { 0 }
                val total1 = k.toDouble()
                val total2 = points.size - total1
                val total = points.size

                for (i in 0 until points.size) {
                    if (i <= k) {
                        matching1[points[i].clusterID]++
                    } else {
                        matching2[points[i].clusterID]++
                    }
                }

                var f1 = matching1[0].toDouble() / total1
                var f2 = matching1[1].toDouble() / total1

                val evi1 = -f1 * log2(f1) - f2 * log2(f2)

                f1 = matching2[0].toDouble() / total2
                f2 = matching2[1].toDouble() / total2

                val evi2 = -f1 * log2(f1) - f2 * log2(f2)

                val cie = (total1 / total) * evi1 + (total2 / total) * evi2
                if (cie < entropyMin) {
                    entropyMin = cie
                    cutPoint = k
                }
            }

            // 5. Apply the cut on the determined point.

            // Set the new ranges.
            rangeSplit[index] = points[cutPoint].value

            val p = Array(2) { 0.0 }

            // Determine the selection probabilities (sum of the utilities / total).
            for (i in 0 until points.size) {
                p[if (i <= cutPoint) 0 else 1] += points[i].util
            }
            probabilitySplit[index] = p[0] / (p[0] + p[1])

            // 6. Remove all legacy information.
            points.clear()
        }
    }

    /**
     * Logarithm to base 2.
     *
     * @param x Input.
     * @return log(x, 2).
     */
    private fun log2(x: Double): Double {
        if (x == 0.0) return 0.0

        return log(x, 2.0)
    }

    /**
     * The parameter util class.
     *
     * @property value Value of the parameter.
     * @property util Utility of the corresponding value.
     * @property clusterID ID (0 or 1) of the cluster the sample is assigned to.
     * @constructor Creates a new instance of ParamterUtil.
     */
    data class ParameterUtil(val value: Double, var util: Double, var clusterID: Int = -1) {
        fun distance(other: ParameterUtil): Double {
            val x = value - other.value
            val y = util - other.util

            return x * x + y * y
        }
    }
}