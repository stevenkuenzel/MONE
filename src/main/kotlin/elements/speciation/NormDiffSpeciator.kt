package elements.speciation

import controlparameters.Parameter
import controlparameters.Parameterized
import difference.DifferenceMetric
import elements.genotype.Genotype
import elements.genotype.neuralnetworks.NetworkGenotype
import elements.phenotype.neuralnetworks.NetworkPhenotype
import util.random.RandomProvider

/**
 * A speciator that works on relative [0, 1] differences.
 *
 * @param T The type of genotype.
 * @constructor Creates a new instance of NormDiffSpeciator.
 *
 * @param parameterized The Parameterized-object, e.g. NEAT.
 * @param differenceMetric The metric that determines the similarity between two genotypes.
 * @param random A random number generator.
 */
class NormDiffSpeciator<T : Genotype<T>>(
    parameterized: Parameterized,
    differenceMetric: DifferenceMetric<T>,
    random: RandomProvider
) :
    Speciator<T>(
        parameterized,
        differenceMetric, random,
        true, true, false
    ) {

    override fun speciate_(set: List<T>, species: MutableList<Species<T>>) {
        // Determine and normalize the distances between the solutions and species representatives.
        determineDistances(set)

        // Create a map of network id -> array index.
        val indexMap = hashMapOf<Int, Int>()

        for (index in set.indices) {
            indexMap[set[index].id] = index
        }

        // Remove old species that are not represented in indexMap.
        species.removeAll { !indexMap.containsKey(it.representative.id) }


        // Apply the normalized difference values in the specietion procedure suggested by Stanley.
        for (i in set.indices) {
            val genome = set[i]

            var foundSpecies = false

            for (spec in species) {
                val diff = if (genome == spec.representative) 0.0 else normDifferences[indices[getIndex(
                    i,
                    indexMap[spec.representative.id]!!,
                    set.size
                )]]

                if (diff <= parameterized.get(Parameter.Speciation_Coefficient)) {
                    foundSpecies = true

                    spec.add(genome)
                    break
                }
            }

            if (!foundSpecies) {
                species.add(Species(nextSpeciesID++, genome, random))
            }
        }
    }

    /**
     * Stores the normalized differences between all solutions (including species representatives).
     */
    var normDifferences = Array(0) { 0.0 }

    /**
     * Indices
     */
    var indices = Array(0) { 0 }

    /**
     * Converts a 2-dim index (a, b) into a 1-dim equivalent.
     *
     * @param a First component.
     * @param b Second component.
     * @param setSize Number of columns.
     * @return The 1-dim equivalent of (a, b).
     */
    private fun getIndex(a: Int, b: Int, setSize: Int): Int {
        return a * setSize + b
    }

    /**
     * Determines the pairwise differences and saves the normalized distance into _normDifferences_.
     *
     * @param set The solutions to consider.
     */
    private fun determineDistances(set: List<T>) {

        normDifferences = Array((set.size * (set.size - 1)) / 2) { 0.0 }
        indices = Array(set.size * set.size) { -1 }


        var differenceMin = Double.MAX_VALUE
        var differenceMax = 0.0

        var indexCounter = 0

        // Determine pairwise differences, as well as min and max distance.
        for (i in set.indices) {
            for (j in i + 1 until set.size) {
                val difference = differenceMetric.getDifference(set[i], set[j])

                if (difference < differenceMin) differenceMin = difference
                if (difference > differenceMax) differenceMax = difference

                normDifferences[indexCounter] = difference

                indices[getIndex(i, j, set.size)] = indexCounter
                indices[getIndex(j, i, set.size)] = indexCounter

                indexCounter++
            }
        }

        // Normalize the differences.
        for (index in normDifferences.indices) {
            normDifferences[index] = (normDifferences[index] - differenceMin) / (differenceMax - differenceMin)
        }
    }
}