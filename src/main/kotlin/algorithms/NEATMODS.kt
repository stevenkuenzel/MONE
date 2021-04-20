package algorithms

import controlparameters.Parameter
import difference.decisionspace.NetworkDifference
import elements.genotype.neuralnetworks.NetworkGenotype
import elements.speciation.Species
import elements.speciation.StanleySpeciator
import experiments.Experiment
import org.apache.commons.math3.util.FastMath
import sorting.impl.CrowdingDistance
import sorting.impl.NondominatedRanking


/**
 * NEAT-MODS.
 *
 * SOURCE: Omer Abramovich, and Amiram Moshaiov. "Multi-objective topology and weight evolution of neuro-controllers." 2016 IEEE congress on evolutionary computation (CEC). IEEE, 2016.
 *
 * @constructor Creates a new instance of NEAT-MODS.
 *
 * @param experiment The experiment instance.
 */
class NEATMODS(experiment: Experiment) : NEAT(experiment, NondominatedRanking(CrowdingDistance())) {

    init {
        speciator = StanleySpeciator(this, NetworkDifference(this), random)
    }

    override fun epoch_() {
        updateExportSet()

        // Default NEAT speciation. Based on the resulting species set, the offspring is created.
        species = speciator.speciate(population, species)

        determineNumberOfChildrenToSpawn(getAsInt(Parameter.Population_Size))

        // Create offspring.
        for (spec in species) {
            while (spec.canSpawn()) {
                population.add(variation(spec.selectParents(get(Parameter.Selection_Pressure))))
            }
        }

        evaluate()

        sortPopulation()

        // NEAT-MODS speciation to select the survivors (Phase E + F: Selection + Save Parent Population).
        species = speciator.speciate(population, species)

        selectSurvivors()
    }

    /**
     * NEAT-MODS survivor selection. First determine the best q species, that are allowed to contribute surviving solutions. Then iterate over those to select the actual survivors.
     *
     */
    private fun selectSurvivors() {

        val q =
            FastMath.floor(getAsInt(Parameter.Population_Size).toDouble() / species[0].elements[0].fitness!!.size.toDouble())
                .toInt()

        // Select the species allowed to add their members to the next generation.
        val survivingSpecies: MutableList<Species<NetworkGenotype>>

        if (species.size < q) {
            survivingSpecies = species.toMutableList()
        } else {
            var added = 0
            survivingSpecies = mutableListOf()

            val speciesMap = hashMapOf<NetworkGenotype, Species<NetworkGenotype>>()
            species.forEach { x -> x.elements.forEach { y -> speciesMap[y] = x } }

            for (genome in population) {
                val spec = speciesMap[genome]!!

                if (!survivingSpecies.contains(spec)) {
                    survivingSpecies.add(spec)
                    added += spec.size()

                    if (survivingSpecies.size >= q && added >= getAsInt(Parameter.Population_Size)) {
                        break
                    }
                }
            }
        }

        // Select next generation.
        population.clear()
        var line = 0

        val populationSize = getAsInt(Parameter.Population_Size)

        // Iterate 'line-wise' over the selected species.
        while (population.size < populationSize) {
            for (spec in survivingSpecies) {
                if (spec.size() > line) {
                    population.add(spec.get(line))

                    if (population.size == populationSize) {
                        break
                    }
                }
            }

            line++
        }
    }
}