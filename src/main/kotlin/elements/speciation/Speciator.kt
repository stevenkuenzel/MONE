package elements.speciation

import controlparameters.Parameter
import controlparameters.Parameterized
import difference.DifferenceMetric
import elements.genotype.Genotype
import org.apache.commons.math3.util.FastMath
import util.random.RandomProvider
import kotlin.math.abs

/**
 * Sorts a list of solutions into species of similar (w.r.t. to a certain measure) solutions.
 *
 * @param T The type of genotype.
 * @property parameterized The Parameterized-object, e.g. NEAT.
 * @property differenceMetric The metric that determines the similarity between two genotypes.
 * @property random A random number generator.
 * @property resetMapping Reset the existing mapping from solution to species before every iteration?
 * @property removeStagnating Remove species that do not show an improvement among their members for a certain period?
 * @property autoThreshold Determine the distance threshold (for two solutions belonging to the same species) automatically to match a certain number of target species? Note that the number of species is determined according to the parameter _Speciation_Coefficient_ then. If false, the parameter _Speciation_Coefficient_ determines the relative threshold.
 * @constructor Creates a new instance of Speciator.
 */
abstract class Speciator<T : Genotype<T>>(val parameterized: Parameterized, val differenceMetric: DifferenceMetric<T>, val random : RandomProvider, val resetMapping : Boolean = true, val removeStagnating : Boolean = true, val autoThreshold : Boolean = true) {
    /**
     * ID of the next species.
     */
    var nextSpeciesID = 0

    /**
     * The desired number of species to create.
     */
    var targetNumOfSpecies = -1

    /**
     * The difference threshold for two solutions belonging to the same species.
     */
    var currentThreshold = 0.5


    /**
     * Sorts the solutions of _set_ into species.
     *
     * @param set The solutions to sort.
     * @param species The initial and (at termination) resulting species.
     */
    abstract fun speciate_(set: List<T>, species: MutableList<Species<T>>)

    /**
     *Sorts the solutions of _set_ into specie.
     *
     * @param set The solutions to sort.
     * @param species The initial species existing.
     * @return The new species.
     */
    fun speciate(
        set: List<T>,
        species: MutableList<Species<T>>
    ): MutableList<Species<T>> {
        if (species.isNotEmpty()) {
            if (removeStagnating) {
                val maxStagnation = parameterized.get(Parameter.Maximum_Stagnation)

                species.removeAll { it.generationsWithoutImprovement > maxStagnation }
            }

            if (resetMapping) {
                species.forEach { it.clear() }
            }
        }

        if (autoThreshold) {
            // Determine the desired number of species and the necessary threshold value.
            targetNumOfSpecies = FastMath.max(
                2,
                FastMath.floor(
                    (parameterized.getAsInt(Parameter.Population_Size) / 2).toDouble() * parameterized.get(
                        Parameter.Speciation_Coefficient
                    )
                ).toInt()
            )

            findThreshold(set, species, targetNumOfSpecies)
        } else {
            // Consider the parameter value of _Speciation_Coefficient_ as relative threshold.
            targetNumOfSpecies = -1
            currentThreshold = parameterized.get(Parameter.Speciation_Coefficient)
        }


        // Do the actually sorting.
        speciate_(set, species)

        // Remove empty species, if existing.
        species.removeAll { x -> x.size() == 0 }

        return species
    }

    /**
     * Determines an adequate threshold in order to match a certain number of species after termination.
     *
     * @param set The solutions to sort.
     * @param species The existing species.
     * @param numOfSpecies The target number of species.
     * @param numOfTries The maximum number of iterations.
     */
    fun findThreshold(
        set: List<T>,
        species: MutableList<Species<T>>,
        numOfSpecies: Int,
        numOfTries: Int = 100
    ) {
        var remainingIterations = numOfTries

        // Create a copy of the existing species.
        val speciesCpy = mutableListOf<Species<T>>()
        species.forEach { speciesCpy.add(Species(-1,it.representative, random)) }

        // Try to find a threshold that leads to the desired number of species (+- 1).
        while (remainingIterations-- > 0) {
            val speciesTemp = speciesCpy.toMutableList()
            val cpy = set.toMutableList()

            // Speciate the copied solutions.
            speciate_(cpy, speciesTemp)

            // Determine the difference and adapt the threshold value.
            val diffTarget = speciesTemp.size - numOfSpecies

            if (diffTarget < 0) {
                // To less species, decrease threshold.
                currentThreshold *= 0.9
            } else if (diffTarget > 0) {
                // To many species, increase threshold.
                currentThreshold *= 1.1
            }

            if (abs(diffTarget) <= 1)
            {
                break
            }
        }
    }
}