package controlparameters.controllers

import controlparameters.Parameterized

/**
 * Sets all parameter values to their repsective default values. The values remain fixed throughout the evolutionary process.
 *
 * @constructor Creates a new instance of DefaultParameterValues.
 *
 * @param parameterized The instance of the Parmaterized-objective, i.e. E(MO)A, to control.
 */
class DefaultParameterValues(parameterized: Parameterized) :
    ParameterController(parameterized, null) {
    override val requiresUpdate = false

    override fun set() {
        for (parameter in parameters) {
            val range = parameter.max - parameter.min
            val defaultOffset = parameter.default - parameter.min

            parameterized.set(parameter, defaultOffset / range)
        }
    }

    override fun update(quality: Double) {
        // Update is not necessary here.
    }
}