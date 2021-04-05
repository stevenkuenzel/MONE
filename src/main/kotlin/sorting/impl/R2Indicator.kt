package sorting.impl

import org.apache.commons.math3.util.FastMath
import sorting.QProcedure
import sorting.ReferenceBasedQP
import util.weights.HammersleyWeights
import kotlin.math.abs

/**
 * R2 Indicator. Based on the implementation by Kuenzel and Meyer-Nieberg:
 *
 * SOURCE: Kuenzel, Steven, and Silja Meyer-Nieberg. "Coping with opponents: multi-objective evolutionary neural networks for fighting games." Neural Computing and Applications (2020): 1-32.
 *
 * @constructor Creates a new instance.
 *
 * @param iterative If true, the q-Procedure runs in iterative mode.
 * @param next The subordinate q-Procedure. Not mandatory.
 */
class R2Indicator(iterative: Boolean, next: QProcedure?) : ReferenceBasedQP(iterative, next) {

    constructor(iterative: Boolean) : this(iterative, null)
    constructor() : this(false, null)

    init {
        normMinValue = 1.0
    }

    override val key: Int
        get() = 1
    override val name: String
        get() = "R2 Indicator"
    override val nameShort: String
        get() = "R2"
    override val normalizeDuringEvolution: Boolean
        get() = true

    /**
     * Number of weight vectors.
     */
    var numOfWeightVectors = 100

    /**
     * Weight vector generator.
     */
    private val weightGenerator = HammersleyWeights()

    override fun computeValue(matrix: Array<Array<Double>>): Double {
        val numOfObjectives = matrix[0].size

        if (updateEveryCall) updateReferencePoint(numOfObjectives, 0.0)
        return computeR2(matrix, weightGenerator.getWeightVectors(numOfWeightVectors, numOfObjectives))
    }

    override fun computeContribution(matrix: Array<Array<Double>>): Array<Double> {
        val numOfObjectives = matrix[0].size

        if (updateEveryCall) updateReferencePoint(numOfObjectives, 0.0)

        return computeContributions(matrix, weightGenerator.getWeightVectors(numOfWeightVectors, numOfObjectives))
    }

    override fun updateReferencePoint(point: Array<Double>, setSize: Int) {
        referencePointCorrectionFactor = 1.0
        referencePoint = point
    }


    /**
     * Computes the indices (i in P) of the Individuals that perform best for each weight vector (l in L). To save
     * computation time compute the R2 value for each weight vector if the best (or second best) individual is applied.
     *
     * @param population The Population P.
     * @return The R2 value of the whole Population P.
     */
    private fun computeContributions(
        population: Array<Array<Double>>,
        weightVectors: Array<Array<Double>>
    ): Array<Double> {

        assert(population.size > 1)

        val numOfObjectives = population[0].size
        val result = Array(population.size) { 0.0 }

        for (lambda in weightVectors) {
            // Best (lambdaFirst) and second best (lambdaSecond) value.
            var lambdaFirst = Double.MAX_VALUE
            var lambdaSecond = Double.MAX_VALUE

            // Index of the Individual (i in P) resulting in the best value for vector (l in L).
            var indexMin = -1

            for (i in population.indices) {
                val point = population[i]

                // Compute the maximum product of the current point (differenceMeasure with reference point) and the current
                // weight vector.
                var max = lambda[0] * abs(point[0] - referencePoint[0])

                // If not max < lambdaSecond, point can not be the best or second best Individual for vector (l in L).
                if (max < lambdaSecond) {

                    // Additional improvement: If value or max >= lambdaSecond, then the Individual (i in P) can not be the
                    // best or second best Individual for vector (l in L). This can save up to ~ (k - 1) / k percent
                    // of all computations. Practice has shown that 20 - 25 % of all computations are saved by this
                    // procedure.
                    var valueToLarge = false

                    for (k in 1 until numOfObjectives) {
                        val value = lambda[k] * abs(point[k] - referencePoint[k])

                        // If not value < lambdaSecond, point can not be the best or second best Individual for vector (l in L).
                        if (value >= lambdaSecond) {
                            valueToLarge = true
                            break
                        }

                        if (value > max) {
                            max = value
                        }
                    }

                    // Set the value for the best and second best Individual known yet.
                    if (!valueToLarge) {
                        if (max < lambdaFirst) {
                            lambdaSecond = lambdaFirst

                            lambdaFirst = max
                            indexMin = i
                        } else if (max < lambdaSecond) {
                            lambdaSecond = max
                        }
                    }
                }
            }

            result[indexMin] += lambdaSecond - lambdaFirst
        }

        for (i in result.indices) {
            result[i] /= weightVectors.size.toDouble()
        }

        return result
    }

    /**
     * Computes the R2 value of a matrix of objective vectors for certain weights.
     */
    private fun computeR2(
        population: Array<Array<Double>>,
        weightVectors: Array<Array<Double>>
    ): Double {
        val numOfObjectives = population[0].size
        var sum = 0.0

        for (vector in weightVectors) {
            var min = Double.MAX_VALUE

            for (point in population) {
                var max = Double.NEGATIVE_INFINITY

                for (k in 0 until numOfObjectives) {
                    val value = vector[k] * FastMath.abs(referencePoint[k] - point[k])

                    if (value > max) {
                        max = value
                    }
                }

                if (max < min) {
                    min = max
                }
            }

            sum += min
        }

        return sum / weightVectors.size.toDouble()
    }
}

