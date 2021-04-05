package controlparameters.controllers

import controlparameters.Parameter
import controlparameters.ParameterScope
import controlparameters.Parameterized
import util.random.RandomProvider

/**
 * A Parameter controller.
 *
 * @property parameterized The EA (or Parameterized) instance to control.
 * @property random A random number generator.
 * @constructor Creates an instance of ParameterController.
 */
abstract class ParameterController(val parameterized: Parameterized, val random: RandomProvider?) {

    val parameters = parameterized.getRegisteredParameters(ParameterScope.Operation, ParameterScope.Generation)
    var neverUpdatedYet = true

    /**
     * Determines whether the parameter controller updates the parameter values every genration.
     */
    abstract val requiresUpdate : Boolean

    /**
     * Sets a new parameter vector to the EA.
     *
     */
    abstract fun set()

    /**
     * Updates the parameter utilities. Procedure specific.
     *
     * @param quality Measure of a certain quality metric that contributes to the utility of parameter vectors.
     */
    abstract fun update(quality: Double)

    /**
     * Updates the parameter values and sets a parameter vector to the EA.
     *
     * @param quality Measure of a certain quality metric that contributes to the utility of parameter vectors.
     */
    fun updateAndSetValues(quality: Double) {
        update(quality)
        set()
    }
}