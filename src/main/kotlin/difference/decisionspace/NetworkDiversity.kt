package difference.decisionspace

import difference.DiversityMetric
import elements.genotype.Genotype
import elements.genotype.neuralnetworks.NetworkGenotype
import elements.phenotype.neuralnetworks.NetworkPhenotype

/**
 * Diversity of a list of neural networks (Stanley / NEAT encoding). Considers all occurring innovations.
 *
 * @constructor Creates a new instance.
 */
class NetworkDiversity : DiversityMetric<NetworkGenotype>() {
    override val name = "Decision Space"
    override val nameShort = "DS"
    override val id = 0

    override fun getData(set: List<NetworkGenotype>): Array<Array<Double>> {
        // Determine the innovations and store all link weights for each innovation in a list.
        val innovMap = hashMapOf<Int, MutableList<Double>>()

        for (neatGenome in set) {
            for (link in neatGenome.links) {
                if (!innovMap.containsKey(link.innovationID))
                {
                    innovMap[link.innovationID] = mutableListOf(link.weight)
                }
                else
                {
                    innovMap[link.innovationID]!!.add(link.weight)
                }
            }
        }

        val keys = innovMap.keys.toList().sorted()

        for (key in keys) {
            val list = innovMap[key]!!
            // If an innovation does only occur once, add the neutral element to let the innovation contribute a certain diversity.
            if (list.size == 1) list.add(getNeutralElement())

            // Normalize all values between 0 and 1.
            val min = list.minOrNull()!!
            val max = list.maxOrNull()!!
            val diff = max - min

            if (diff > 0.0)
            {
                for (index in list.indices) {
                    list[index] = (list[index] - min) / diff
                }
            }
        }

        // Return an array of normalized link weights for each innovation id.
        return Array(keys.size) { i -> innovMap[keys[i]]!!.toTypedArray()}
    }

    override fun getNeutralElement(): Double {
        return 1.0
    }

    override fun copy(): DiversityMetric<NetworkGenotype> {
        return NetworkDiversity()
    }


//    /**
//     * EXPERIMENTAL. Not part of my thesis.
//     *
//     * @param set
//     * @return
//     */
//    override fun getData2(set: List<NetworkGenotype>): Array<Array<Double>> {
//        assert(set.isNotEmpty())
//
//        val innovationSet = mutableSetOf<Int>()
//
//        for (neatGenome in set) {
//            for (link in neatGenome.links) {
//                innovationSet.add(link.innovationID)
//            }
//        }
//
//        val keys = innovationSet.toList().sorted()
//        val km = hashMapOf<Int, Int>()
//        keys.mapIndexed { index, i -> km[i] = index }
//
//        val result = Array(set.size) { Array(keys.size) {Double.NaN} }
//
//        for (index in set.indices) {
//            val genotype = set[index]
//            for (link in genotype.links) {
//                result[index][km[link.innovationID]!!] = link.weight
//            }
//        }
//
//        return result
//    }
}