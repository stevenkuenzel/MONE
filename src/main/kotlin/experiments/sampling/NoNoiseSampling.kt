package experiments.sampling

import experiments.SampleData

/**
 * Resampling when an experiment is noise-free, i.e., only a single sample is drawn per solution an reference.
 *
 * @constructor Creates a new instance of.
 */
class NoNoiseSampling : SamplingStrategy() {

    override fun canSample(sampleData: SampleData, referenceID: Int): Boolean {
        // Do only allow a single sample.
        return !sampleData.known(referenceID)
    }
}