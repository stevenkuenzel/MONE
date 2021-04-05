package experiments.sampling

import experiments.SampleData

/**
 * Determines how many samples are created per solution and reference.
 *
 * @constructor Creates a new instance.
 */
abstract class SamplingStrategy {

    /**
     * Returns true, if another sample can be created for the solution and the reference. According to the strategy at hand.
     *
     * @param sampleData The current sample data for the solution.
     * @param referenceID The reference ID.
     * @return True, if another sample can be created.
     */
    abstract fun canSample(sampleData: SampleData, referenceID: Int): Boolean
}