package settings

import algorithms.AbstractAlgorithm
import algorithms.NEAT
import controlparameters.controllers.ParameterController
import util.random.RandomProvider

/**
 * Enum of available parameter controllers.
 *
 * @property id The experiment ID.
 * @property create Lambda expression that creates an instance of the respective parameter controller.
 */
enum class EnumParameterController(val id : Int, private val create : (AbstractAlgorithm<*>, RandomProvider) -> ParameterController) {
    EARPC(0, {neat, random -> controlparameters.controllers.EARPC(neat, random) }),
    DDYPC(1, {neat, random -> controlparameters.controllers.DDYPC(neat, random) }),
    Default(2, { neat, _ -> controlparameters.controllers.DefaultParameterValues(neat) }),
    Random(3, {neat, random -> controlparameters.controllers.RandomParameterValues(neat, random) });

    /**
     * Returns a copy of the parameter controller for the given algorithm.
     *
     * @param algorithm The instance of the algorithm.
     * @param random An instance of the random number generator.
     * @return An instance of the parameter controller.
     */
    fun getCopyOfParameterController(algorithm : AbstractAlgorithm<*>, random: RandomProvider) : ParameterController
    {
        return create(algorithm, random)
    }
}