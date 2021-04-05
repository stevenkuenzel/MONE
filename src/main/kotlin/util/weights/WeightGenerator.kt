package util.weights

import java.util.*

/**
 * A weight generator, e.g., for the R2 indicator.
 */
abstract class WeightGenerator {
    /**
     * Stores all generated weight vectors (per dimension).
     */
    private val existingWeightVectors = HashMap<Int, Array<Array<Double>>>()

    /**
     * Returns (and previously generates) the required number of weight vectors.
     *
     * @param numOfVectors The number of weight vectors.
     * @param numOfObjectives The number of dimensions.
     * @return The weight vectors.
     */
    fun getWeightVectors(numOfVectors: Int, numOfObjectives: Int): Array<Array<Double>> {
        if (existingWeightVectors.containsKey(numOfObjectives)) {
            val weights = existingWeightVectors[numOfObjectives]

            if (weights!!.size <= numOfVectors) {
                return weights
            }
        }

        val weights = generateWeights(numOfVectors, numOfObjectives)

        existingWeightVectors[numOfObjectives] = weights

        return weights
    }

    /**
     * Creates the required number of weight vectors.
     *
     * @param numOfVectors The number of weight vectors.
     * @param numOfObjectives The number of dimensions.
     * @return The weight vectors.
     */
    abstract fun generateWeights(numOfVectors: Int, numOfObjectives: Int): Array<Array<Double>>
}