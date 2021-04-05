package difference.objectivespace

import difference.DiversityMetric
import elements.genotype.Genotype
import elements.genotype.neuralnetworks.NetworkGenotype

/**
 * Diversity in objective space. Considers the fitness vectors of the solutions.
 *
 * @param T The genotype class.
 * @constructor Creates a new instance.
 */
class FitnessDiversity<T : Genotype<T>> : DiversityMetric<T>() {
    override val name = "Objective Space"
    override val nameShort = "OS"
    override val id = 1

    override fun getData(set: List<T>): Array<Array<Double>> {
        assert(set.isNotEmpty())

        return Array(set[0].fitness!!.size) { k -> Array(set.size) { i -> set[i].fitness!![k] } }
    }

    override fun getNeutralElement(): Double {
        return 1.0
    }

    override fun copy(): DiversityMetric<T> {
        return FitnessDiversity()
    }

//    override fun getData2(set: List<T>): Array<Array<Double>> {
//        return getData(set)
//    }
}