package algorithms

import experiments.Experiment
import sorting.impl.ParetoStrength

/**
 * NEAT-PS. The algorithm is an exact copy of NEAT with the only difference being the q-Procedure (Pareto Strength) which allows to determine a scalar fitness value from a fitness vector.
 *
 * Source: van Willigen, Willem, Evert Haasdijk, and Leon Kester. "Fast, comfortable or economical: evolving platooning strategies with many objectives." 16th International IEEE Conference on Intelligent Transportation Systems (ITSC 2013). IEEE, 2013.
 *
 * @constructor Creates a new instance of NEAT-PS.
 *
 * @param experiment The experiment instance.
 */
class NEATPS(experiment: Experiment) : NEAT(experiment, ParetoStrength()) {
}