package elements.speciation

import controlparameters.Parameterized
import difference.DifferenceMetric
import elements.genotype.Genotype
import elements.genotype.neuralnetworks.NetworkGenotype
import util.random.RandomProvider

/**
 * A speciator that works on absolute differences. Proposed by Stanley.
 *
 * Source: Stanley, Kenneth Owen. Efficient evolution of neural networks through complexification. Diss. 2004.
 *
 * @param T The type of genotype.
 * @constructor Creates a new instance of StanleySpeciator.
 *
 * @param parameterized The Parameterized-object, e.g. NEAT.
 * @param differenceMetric The metric that determines the similarity between two genotypes.
 * @param random A random number generator.
 */
class StanleySpeciator<T : Genotype<T>>(
    parameterized: Parameterized,
    differenceMetric: DifferenceMetric<T>,
    random: RandomProvider
) : Speciator<T>(
    parameterized,
    differenceMetric, random
) {

    override fun speciate_(set: List<T>, species: MutableList<Species<T>>) {

        // Ensure that there final number of species matches the target number.
        if (targetNumOfSpecies != -1 && species.size > targetNumOfSpecies) {
            species.clear()
        }


        // Iterate over all solutions and existing species. Assign each solution to the first species it matches. If no match was found, create a new species.
        for (genome in set) {
            var foundSpecies = false

            for (spec in species) {
                val diff = differenceMetric.getDifference(genome, spec.representative)

                if (diff <= currentThreshold) {
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
}