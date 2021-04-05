package experiments.conversion

import experiments.SampleData


/**
 * Converts SampleData into a fitness vector.
 *
 * @constructor Creates a new instance of SampleToFitnessConverter.
 */
abstract class SampleToFitnessConverter {

    /**
     * Converts the provided SampleData into a fitness vector.
     *
     * @param sampleData The SampleData instance.
     * @return The fitness vector.
     */
    abstract fun convert(sampleData: SampleData): Array<Double>
}