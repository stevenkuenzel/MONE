package sorting.impl

import sorting.ParetoDominance
import sorting.QProcedure

/**
 * Non-dominated Ranking. Based on the implementation by Deb et al.
 *
 * SOURCE: Deb, Kalyanmoy, et al. "A fast and elitist multiobjective genetic algorithm: NSGA-II." IEEE transactions on evolutionary computation 6.2 (2002): 182-197.
 *
 * @constructor Creates a new instance.
 *
 * @param next The subordinate q-Procedure. Not mandatory.
 */
class NondominatedRanking(next : QProcedure?) : QProcedure(false, next) {

    constructor() : this(null)

    override val key: Int
        get() = 0
    override val name: String
        get() = "Non-dominated Ranking"
    override val nameShort: String
        get() = "NDR"
    override val normalizeDuringEvolution: Boolean
        get() = false

    override fun computeValue(matrix: Array<Array<Double>>): Double {
        if (next != null)
        {
            return next!!.computeValue(matrix)
        }
        return -1.0
    }

    override fun computeContribution(matrix: Array<Array<Double>>): Array<Double> {
        val result = Array(matrix.size) { 0.0 }

        // Sort the solutions in non-dominating fronts.
        val fronts = ParetoDominance.sortInNonDominatedFronts(Array(matrix.size) { i -> Pair(i, matrix[i])})

        for (i in fronts.indices) {
            val front = fronts[i]

            // Set the contribution in proportion to the front index.
            val frontContribution = (fronts.size - i).toDouble()

            for (item in front) {
                result[item.first] = frontContribution
            }
        }

        return result
    }

}