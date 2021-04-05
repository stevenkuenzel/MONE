package difference

import elements.genotype.Genotype

/**
 * A genotypic difference metric.
 *
 *
 * @param T The genotype class.
 * @constructor Creates a new instance.
 */
abstract class DifferenceMetric<T : Genotype<T>> {
    abstract fun getDifference(a : T, b : T) : Double
}