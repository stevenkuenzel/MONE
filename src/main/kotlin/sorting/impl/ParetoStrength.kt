package sorting.impl

import sorting.ParetoDominance
import sorting.QProcedure

/**
 * Pareto Strength. Based on the implementation by Zitzler et al.
 *
 * SOURCE: Zitzler, Eckart, Marco Laumanns, and Lothar Thiele. "SPEA2: Improving the strength Pareto evolutionary algorithm." TIK-report 103 (2001).
 *
 * @constructor Creates a new instance.
 *
 * @param iterative If true, the q-Procedure runs in iterative mode.
 * @param next The subordinate q-Procedure. Not mandatory.
 */
class ParetoStrength(next : QProcedure?) : QProcedure(false, next) {

    constructor() : this(null)

    override val key: Int
        get() = 4
    override val name: String
        get() = "Pareto Strength"
    override val nameShort: String
        get() = "PS"
    override val normalizeDuringEvolution: Boolean
        get() = false

    override fun computeValue(matrix: Array<Array<Double>>): Double {
        return -1.0
    }

    override fun computeContribution(matrix: Array<Array<Double>>): Array<Double> {
        val strength = Array(matrix.size) { 0 }
        val dominanceResults = Array(matrix.size * (matrix.size - 1) / 2) { 0 }

        /**
         * Counts the array index. Avoids 'book-keeping' of the index over two nested loops.
         */
        var testIndex = 0

        // Determine the number of dominated solutions each.
        for (i in 0 until matrix.size - 1) {
            for (j in i + 1 until matrix.size) {
                val comparison = ParetoDominance.dominanceTest(matrix[i], matrix[j])

                dominanceResults[testIndex++] = comparison

                if (comparison < 0) {
                    strength[i]++
                } else if (comparison > 0) {
                    strength[j]++
                }
            }
        }

        testIndex = 0

        val contribution = Array(matrix.size) { 0.0 }

        // Sum the strength values of all dominating solutions each.
        for (i in 0 until matrix.size - 1) {
            for (j in i + 1 until matrix.size) {
                val comparison = dominanceResults[testIndex++]

                if (comparison < 0) {
                    contribution[j] += strength[i].toDouble()
                } else if (comparison > 0) {
                    contribution[i] += strength[j].toDouble()
                }
            }
        }

        // Invert the contribution. Before this step, the solutions with the highest values are considered to be the worst solutions.
        for (i in contribution.indices) {
            contribution[i] = 1.0 / (1.0 + contribution[i])
        }

        // Now, the highest contribution represents the 'best' solution w.r.t. Pareto Strength.
        return contribution
    }
}