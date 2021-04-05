package util.weights

import util.lhd.LatinHypercubeDesign

/**
 * Employs an LHD to create weight vectors.
 *
 */
class LHDWeights : WeightGenerator() {
    override fun generateWeights(numOfVectors: Int, numOfObjectives: Int): Array<Array<Double>> {
        // Return the according LHD as weight vectors.
        return LatinHypercubeDesign.create(numOfObjectives)
    }
}