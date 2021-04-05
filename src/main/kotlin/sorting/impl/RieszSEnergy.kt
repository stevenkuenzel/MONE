package sorting.impl

import sorting.QProcedure
import kotlin.math.pow

/**
 * Riesz s-energy. New implementation.
 *
 * @property s Exponential for determining the s-energy. Default = 1. Higher values increase the 'pressure' between more diverse and similar vectors.
 * @property minSquareDistance The minimum square distance to consider. Default = 0.001. Allows to define an upper bound for s-energy.
 *
 * @constructor Creates a new instance.
 *
 * @param next The subordinate q-Procedure. Not mandatory.
 */

class RieszSEnergy(val s: Double = 1.0, val minSquareDistance: Double = 0.001, next: QProcedure?) :
    QProcedure(false, next) {

    constructor() : this(1.0, 0.001, null)

    override val key: Int
        get() = 5
    override val name: String
        get() = "Inv. Limited Riesz S-energy"
    override val nameShort: String
        get() = "IL-RSE"
    override val normalizeDuringEvolution: Boolean
        get() = true

    override fun computeValue(matrix: Array<Array<Double>>): Double {
        // Return the average contribution of the solutions.
        return computeContribution(matrix).average()
    }

    override fun computeContribution(matrix: Array<Array<Double>>): Array<Double> {
        val K = matrix[0].size
        val contribution = Array(matrix.size) { 0.0 }

        for (i in 0 until matrix.size - 1) {
            for (j in i + 1 until matrix.size) {

                // Determine the squared Euclidean distance between i and j.
                var squareDistance = 0.0

                for (k in 0 until K) {
                    val a = matrix[i][k]
                    val b = matrix[j][k]

                    if (a.isNaN() || b.isNaN()) continue
                    val d = a - b
                    squareDistance += d * d
                }

                // Apply the lower bound.
                if (squareDistance < minSquareDistance) squareDistance = minSquareDistance

                // Determine and add the s-energy.
                val sEnergy = squareDistance.pow(-s)
                contribution[i] += sEnergy
                contribution[j] += sEnergy
            }
        }

        /**
         *Maximum possible s-energy. I.e. a vector that smallest considered distance to all other solutions.
         */
        val cMax = minSquareDistance.pow(-s) * (matrix.size - 1).toDouble()

        // Invert the contribution.
        for (index in contribution.indices) {
            contribution[index] = 1.0 - (contribution[index] / cMax)
        }

        return contribution
    }
}