package experiments.conversion

import experiments.SampleData

/**
 * Returns the vector composing the median values of each objective.
 *
 * @constructor Creates a new instance.
 */
class MedianSampleFitness : SampleToFitnessConverter() {
    override fun convert(sampleData: SampleData): Array<Double> {
        val stat = sampleData.getSampleStatistics() ?: return Array(0) { 1.0 }

        return Array(stat.size) { i -> stat[i].getPercentile(50.0) }
    }
}