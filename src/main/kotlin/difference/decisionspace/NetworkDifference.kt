package difference.decisionspace

import controlparameters.Parameter
import controlparameters.Parameterized
import difference.DifferenceMetric
import elements.genotype.Genotype
import elements.genotype.neuralnetworks.NetworkGenotype
import elements.phenotype.neuralnetworks.NetworkPhenotype
import org.apache.commons.math3.util.FastMath

/**
 * Difference between two neural network genomes (Stanley / NEAT encoding) based on Stanley's approach.
 *
 * Source: Stanley, Kenneth Owen. Efficient evolution of neural networks through complexification. Diss. 2004.
 *
 * @property parameterized The Parameterized-object, i.e. E(MO)A. Necessary to provide the difference coefficients.
 */
class NetworkDifference(val parameterized: Parameterized)  : DifferenceMetric<NetworkGenotype>() {

    override fun getDifference(a: NetworkGenotype, b: NetworkGenotype): Double {

        // Determine Stanley's difference.
        val diff = StanleyDifference(a, b)


        // Sum the difference values with their according relevance / weighting.
        val c1 = parameterized.get(Parameter.Factor_C1_Excess)

        val differentNeurons = (c1 * diff.numExcess.toDouble() +
                parameterized.get(Parameter.Factor_C2_Disjoint) * diff.numDisjoint.toDouble()) / diff.longestGenome.toDouble()
        var differentWeights =
            (parameterized.get(Parameter.Factor_C3_Weight_Difference) * diff.weightDifference) / diff.numCommon.toDouble()

        // Cap the weight differences at the value of Factor_C1_Excess. (Avoids that the weight difference exceeds the other difference values by numbers.
        if (differentWeights > c1) {
            differentWeights = c1
        }

        return (differentNeurons + differentWeights) / 2.0
    }

    /**
     * Difference of two neural networks by Stanley.
     *
     * Source: Stanley, Kenneth Owen. Efficient evolution of neural networks through complexification. Diss. 2004.
     *
     * @property a The first network.
     * @property b The second network.
     * @constructor Creates a new instance and determines the difference values.
     */
    data class StanleyDifference(val a : NetworkGenotype, val b : NetworkGenotype)
    {
        var longestGenome = 0
        var numCommon = 0
        var numDisjoint = 0
        var numExcess = 0
        var weightDifference = 0.0

        init {
            val sizeA = a.links.size
            val sizeB = b.links.size

            longestGenome = if (sizeA > sizeB) sizeA else sizeB

            var i = 0
            var s1 = 0
            var s2 = 0

            while (i < longestGenome + numDisjoint) {
                val i1 = i - s1
                val i2 = i - s2

                if (i1 < sizeA && i2 < sizeB) {
                    if (a.links[i1].innovationID == b.links[i2].innovationID) {
                        weightDifference += FastMath.abs(a.links[i1].weight - b.links[i2].weight)
                        numCommon++
                    } else {
                        if (a.links[i1].innovationID < b.links[i2].innovationID) {
                            s2++
                        } else {
                            s1++
                        }

                        numDisjoint++
                    }
                } else {
                    numExcess = (longestGenome + numDisjoint) - i
                    break
                }

                i++
            }
        }
    }
}