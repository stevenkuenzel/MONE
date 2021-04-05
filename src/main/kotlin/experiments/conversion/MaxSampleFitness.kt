package experiments.conversion

import experiments.SampleData

/**
 * Returns the vector composing the maximum (i.e., worst) values of each objective.
 *
 * @constructor Creates a new instance.
 */
class MaxSampleFitness : SampleToFitnessConverter() {
    override fun convert(sampleData: SampleData): Array<Double> {
        val stat = sampleData.getSampleStatistics() ?: return Array(0) { 1.0 }

        return Array(stat.size) { i -> stat[i].max }
    }
}