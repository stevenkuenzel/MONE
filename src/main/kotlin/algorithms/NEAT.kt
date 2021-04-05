package algorithms

import controlparameters.Parameter
import de.stevenkuenzel.xml.XElement
import difference.decisionspace.NetworkDifference
import elements.genotype.neuralnetworks.NetworkGenotype
import elements.innovation.InnovationManager
import elements.speciation.NormDiffSpeciator
import elements.speciation.Speciator
import elements.speciation.Species
import experiments.Experiment
import org.apache.commons.math3.util.FastMath
import settings.SettingManager
import sorting.QProcedure
import sorting.impl.SingleObjective

/**
 * NEAT.
 *
 * Source: Stanley, Kenneth Owen. Efficient evolution of neural networks through complexification. Diss. 2004.
 *
 * @constructor Creates a new instance of NEAT.
 *
 * @param experiment The experiment instance.
 * @param qProcedure The q-Procedure for sorting.
 */
open class NEAT(experiment: Experiment, qProcedure: QProcedure = SingleObjective()) :
    AbstractAlgorithm<NetworkGenotype>(experiment, qProcedure) {
    companion object
    {
        val SPECIES_CONTRIBUTION_ATTRIBUTE_ID = 6538
    }

    /**
     * The innovation manager instance for this NEAT instance.
     */
    val innovationManager = InnovationManager(experiment.numOfInputs, experiment.numOfOutputs, experiment.bias, random)

    /**
     * The speciator sorts the solutions into species. By default the NormDiffSpeciator (unlike to Stanley's variant) is used.
     */
    var speciator: Speciator<NetworkGenotype> = NormDiffSpeciator(this, NetworkDifference(this), random)

    /**
     * The list of all species currently existing.
     */
    var species = mutableListOf<Species<NetworkGenotype>>()

    override fun registerParameters() {
        register(Parameter.Population_Size, 0.0)
        register(Parameter.Max_Evaluations, SettingManager.global.get("EVALUATIONS_MAX").getValueAsDouble())
        register(Parameter.Weight_Mutation_Range)
        register(Parameter.Prb_Add_Link)
        register(Parameter.Prb_Add_Neuron)
        register(Parameter.Prb_Modify_Weight)
        register(Parameter.Maximum_Stagnation)
        register(Parameter.Speciation_Coefficient)
        register(Parameter.Factor_C1_Excess)
        register(Parameter.Factor_C2_Disjoint)
        register(Parameter.Factor_C3_Weight_Difference)
        register(Parameter.Prb_Crossover_Interspecies)
        register(Parameter.Selection_Pressure)
        register(Parameter.Prb_Mutation)
        register(Parameter.Prb_Crossover)
        register(Parameter.Prb_Cross_Gene_By_Choosing)
        register(Parameter.Prb_Gene_Enabled_On_Crossover)
    }

    override fun initializePopulation_() {
        val numOfSolutions = getAsInt(Parameter.Population_Size)

        for (i in 0 until numOfSolutions) {
            population.add(
                NetworkGenotype(
                    innovationManager,
                    nextGenomeID++,
                    experiment.numOfInputs,
                    experiment.numOfOutputs,
                    experiment.bias,
                    get(Parameter.Weight_Mutation_Range)
                ).also {
                    it.experimentID = experiment.id
                    it.generation = 0
                }
            )
        }
    }

    override fun run_() {
        initializePopulation()
        evaluate()

        sortPopulation()

        while (!terminate()) {
            epoch()
        }

        updateExportSet()
        export()
    }

    override fun epoch_() {
        updateExportSet()

        species = speciator.speciate(population, species)
        determineNumberOfChildrenToSpawn()

        // If too many species existing, discard all species to avoid stagnation of the algorithm.
        val discardAllSpeciesRepresentatives = species.size == population.size

        // Create offspring.
        population.clear()

        for (spec in species) {
            if (!discardAllSpeciesRepresentatives) population.add(spec.representative)

            while (spec.canSpawn()) {
                if (species.size > 1 && random.nextDouble() <= get(Parameter.Prb_Crossover_Interspecies)) {
                    val other = species[random.nextInt(species.size)]
                    population.add(
                        variation(
                            spec.selectSingleSolution(get(Parameter.Selection_Pressure)),
                            other.selectSingleSolution(get(Parameter.Selection_Pressure))
                        )
                    )
                } else {
                    population.add(variation(spec.selectParents(get(Parameter.Selection_Pressure))))
                }
            }
        }

        evaluate()
        sortPopulation()
    }

    override fun evaluate_() : Int {
        return experiment.evaluate(population)
    }



    override fun cross(a: NetworkGenotype, b: NetworkGenotype): NetworkGenotype {
        return a.crossWith(
            b, get(Parameter.Prb_Cross_Gene_By_Choosing), get(Parameter.Prb_Gene_Enabled_On_Crossover), nextGenomeID++
        )
    }

    override fun mutate(a: NetworkGenotype) {
        a.mutate(
            get(Parameter.Prb_Add_Link),
            get(Parameter.Prb_Add_Neuron),
            get(Parameter.Prb_Modify_Weight),
            get(Parameter.Weight_Mutation_Range)
        )
    }

    protected fun determineNumberOfChildrenToSpawn(freeSlots: Int = getAsInt(Parameter.Population_Size) - (if (species.size == population.size) 0 else species.size)) {

        // Determine the q-values of each species (= sum of their respective members q-values).
        species.forEach { spec -> spec.setAttribute(SPECIES_CONTRIBUTION_ATTRIBUTE_ID, spec.elements.sumByDouble { it.qValue }) }

        // Determine the sum of all species' q-values to set the proportion of solutions to spawn.
        val totalSum = species.sumByDouble { x -> x.getAttribute(SPECIES_CONTRIBUTION_ATTRIBUTE_ID) }

        // Set amount to spawn for each species.
        var totalAmountToSpawn = 0

        for (spec in species) {
            spec.amountToSpawn = FastMath.floor(freeSlots.toDouble() * spec.getAttribute(SPECIES_CONTRIBUTION_ATTRIBUTE_ID) / totalSum).toInt()
            totalAmountToSpawn += spec.amountToSpawn
        }

        // Fill remaining slots randomly.
        val remainingSlots = freeSlots - totalAmountToSpawn

        for (i in 0 until remainingSlots) {
            species[random.nextInt(species.size)].amountToSpawn++
        }
    }

    override fun addXElementsToExport(): List<XElement> {
        return listOf(innovationManager.toXElement())
    }
}