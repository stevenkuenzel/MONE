package experiments.sampling

import experiments.Experiment
import experiments.SampleData
import org.apache.commons.math3.util.FastMath

/**
 * Multi-objective Standard Error Dynamic Resampling.
 *
 * @property minSamples Minimum number of samples.
 * @property maxSamples Maximum number of samples.
 * @constructor Creates a new instance.
 *
 * @param experiment The experiment instance.
 *
 * BASED ON: Siegmund, Florian, Amos HC Ng, and Kalyanmoy Deb. "Standard error dynamic resampling for preference-based evolutionary multi-objective optimization." submitted to Computational Optimization and Innovation Laboratory (2016): 1-13.
 */
class StandardErrorDynResampling(experiment: Experiment, val minSamples: Int, val maxSamples: Int) :
    SamplingStrategy() {

    val sethMin = 0.0
    val alpha = 2.0
    var sethMax = 1.0 / 30.0
    var p = 0.0

    var maxStdErr = 1.0

    init {
        // Listen to the onEvaluationStarted event.
        experiment.onEvaluationStarted += {
            p = it.progress
            maxStdErr = FastMath.pow(1.0 - p, alpha) * (sethMax - sethMin) + sethMin
        }
    }

    override fun canSample(sampleData: SampleData, referenceID: Int): Boolean {
        val fSize = sampleData.size(referenceID)

        if (fSize < minSamples) {
            return true
        }

        if (fSize >= maxSamples) {
            return false
        }

        // Perform SEDR.
        if (sampleData.getStandardError(referenceID) > maxStdErr) {
            return true
        }

        return false
    }
}