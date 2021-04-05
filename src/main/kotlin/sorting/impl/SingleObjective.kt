package sorting.impl

import sorting.QProcedure

/**
 * Single-objective. Does only consider the first scalar of each vector.
 *
 * @constructor Creates a new instance.
 *
 * @param next The subordinate q-Procedure. Not mandatory.
 */
class SingleObjective(next : QProcedure?) : QProcedure(false, next) {

    constructor() : this(null)

    override val key: Int
        get() = 6
    override val name: String
        get() = "Single-Objective"
    override val nameShort: String
        get() = "SO"
    override val normalizeDuringEvolution: Boolean
        get() = true

    override fun computeValue(matrix: Array<Array<Double>>): Double {
        // Not defined.
        return -1.0
    }

    override fun computeContribution(matrix: Array<Array<Double>>): Array<Double> {
        // Invert, as q-values are intended to be maximized.
        return Array(matrix.size) { i -> 1.0 - matrix[i][0] }
    }
}