package difference.objectivespace

import difference.DifferenceMetric
import elements.genotype.Genotype
import elements.genotype.neuralnetworks.NetworkGenotype

/**
 * Fitness difference of two genomes. Applies the squared euclidean distance of two points in objective space.
 *
 * @param T The genotype class.
 * @constructor Creates a new instance.
 */
class FitnessDifference<T : Genotype<T>> : DifferenceMetric<T>() {
    override fun getDifference(a: T, b: T): Double {
        var difference = 0.0

        for (k in a.fitness!!.indices) {
            val diff = a.fitness!![k] - b.fitness!![k]

            difference += diff * diff
        }

        return difference
    }
}