package sorting.impl


import sorting.QProcedure
import sorting.equalsDelta
import sorting.notEqualsDelta
import java.util.*


/**
 * Crowding Distance. Based on the implementation by Deb et al.
 *
 * SOURCE: Deb, Kalyanmoy, et al. "A fast and elitist multiobjective genetic algorithm: NSGA-II." IEEE transactions on evolutionary computation 6.2 (2002): 182-197.
 *
 * @constructor Creates a new instance.
 *
 * @param next The subordinate q-Procedure. Not mandatory.
 */
class CrowdingDistance(next: QProcedure?) : QProcedure(false, next) {

    constructor() : this(null)

    override val key: Int
        get() = 2
    override val name: String
        get() = "Crowding Distance"
    override val nameShort: String
        get() = "CD"
    override val normalizeDuringEvolution: Boolean
        get() = true

    override fun computeValue(matrix: Array<Array<Double>>): Double {
        return computeContribution(matrix).average()
    }

    override fun computeContribution(matrix: Array<Array<Double>>): Array<Double> {

        if (matrix.size < 3) {
            return Array(matrix.size) { 1.0 }
        }

        // Preparation.
        val elements = ArrayList<VarPair<Double, Array<Double>>>(matrix.size)

        for ((index, vector) in matrix.withIndex()) {

            elements.add(VarPair(0.0, vector, index))
        }

        // Iterate over all dimensions.
        for (k in elements[0].second.indices) {
            elements.sortBy { x -> x.second[k] }

            val diff = elements.last().second[k] - elements.first().second[k]

            // Are all values equal?
            if (diff.equalsDelta(0.0)) continue

            // Add a value of one to the extremes. In the original implementation those take a value of infinity.
            elements.first().first += 1.0
            elements.last().first += 1.0

            // Determine the crowding values for the remaining vectors.
            for (i in 1 until elements.size - 1) {
                val distance =(elements[i + 1].second[k] - elements[i - 1].second[k]) / diff
                elements[i].first += distance
            }
        }

        // Save the resulting values.
        val result = Array(matrix.size) { 0.0 }

        for (element in elements) {
            result[element.index] = element.first
        }

        return result
    }

    /**
     * Links two object instances.
     */
    data class VarPair<V, U>(var first: V, var second: U, val index: Int)
}