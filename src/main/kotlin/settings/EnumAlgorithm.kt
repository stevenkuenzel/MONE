package settings

import algorithms.AbstractAlgorithm
import algorithms.NEAT
import elements.genotype.Genotype
import experiments.Experiment
import sorting.QProcedure

/**
 * Enum of available algorithms.
 *
 * @property id The algorithm ID.
 * @property create Lambda expression that creates an instance of the respective algorithm.
 */
enum class EnumAlgorithm(val id: Int, private val create: (Experiment, QProcedure?) -> AbstractAlgorithm<*>) {
    nNEAT(0, { experiment, qProcedure -> algorithms.nNEAT(experiment, qProcedure!!) }),
    NEAT_MODS(1, { experiment, _ -> algorithms.NEATMODS(experiment) }),
    NEAT_PS(2, { experiment, _ -> algorithms.NEATPS(experiment) }),

    /*
    EXPERIMENTAL / FUTURE WORK. NOT PART OF MY THESIS.
     */

    EMOA(3, {experiment, qProcedure -> algorithms.EMOA(experiment as experiments.testproblems.TestProblem, qProcedure!!) }),
    ANNEMOA(4, {experiment, qProcedure -> algorithms.ANNEMOA(experiment, qProcedure!!) });

    /**
     * Returns a copy of the algorithm for the given experiment.
     *
     * @param experiment The experiment instance.
     * @param qProcedure The q-Procedure (only nNEAT and EMOA).
     * @return An instance of the algorithm.
     */
    fun getCopyOfAlgorithm(experiment: Experiment, qProcedure: QProcedure?): AbstractAlgorithm<*> {
        return create(experiment, qProcedure)
    }
}