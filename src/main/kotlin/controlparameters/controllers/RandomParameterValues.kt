package controlparameters.controllers

import controlparameters.Parameterized
import util.random.RandomProvider

/**
 * Sets all parameter values to random values. The values remain fixed throughout the evolutionary process.
 *
 * @constructor Creates a new instance of RandomParameterValues.
 *
 * @param parameterized The instance of the Parmaterized-objective, i.e. E(MO)A, to control.
 * @param random A random provider.
 */
class RandomParameterValues(parameterized: Parameterized, random: RandomProvider) : ParameterController(
    parameterized,
    random
) {
    override val requiresUpdate = false

    override fun set() {
        for (parameter in parameters) {
            parameterized.set(parameter, random!!.nextDouble())
        }
    }

    override fun update(quality: Double) {
        // Update is not necessary here.
    }
}