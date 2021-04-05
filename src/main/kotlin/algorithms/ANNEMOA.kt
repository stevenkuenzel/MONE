package algorithms

import controlparameters.Parameter
import de.stevenkuenzel.xml.XElement
import elements.genotype.neuralnetworks.NetworkGenotype
import elements.genotype.newannencoding.SKNet
import experiments.Experiment
import org.apache.commons.math3.util.FastMath
import org.apache.commons.math3.util.Precision
import settings.SettingManager
import sorting.QProcedure
import util.Selection
import kotlin.math.exp
import kotlin.math.max

class ANNEMOA(experiment: Experiment, qProcedure: QProcedure) : AbstractAlgorithm<SKNet>(experiment, qProcedure) {


    override fun registerParameters() {
        register(Parameter.Population_Size, 0.0)
        register(Parameter.Max_Evaluations, SettingManager.global.get("EVALUATIONS_MAX").getValueAsDouble())
        register(Parameter.Weight_Mutation_Range)
        register(Parameter.Prb_Add_Link)
        register(Parameter.Prb_Add_Neuron)
        register(Parameter.Prb_Remove_Link)
        register(Parameter.Prb_Modify_Weight)
        register(Parameter.Replacement_Rate)

        register(Parameter.Selection_Pressure)
        register(Parameter.Prb_Mutation)
        register(Parameter.Prb_Crossover)
        register(Parameter.Prb_Cross_Gene_By_Choosing)
    }

    override fun initializePopulation_() {
        val numOfSolutions = getAsInt(Parameter.Population_Size)

        for (i in 0 until numOfSolutions) {
            population.add(SKNet(nextGenomeID++, experiment.numOfInputs, experiment.numOfOutputs).also {
                    it.experimentID = experiment.id
                    it.generation = 0
                }.also { it.createBase(get(Parameter.Weight_Mutation_Range)) }
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

        // Determine the number of children and create those through variation.
        val numOfChildren = max(
            1, Precision.round(
                get(Parameter.Replacement_Rate) * get(Parameter.Population_Size),
                0
            ).toInt()
        )

        // Create the according number of offspring.
        for (i in 0 until numOfChildren) {
            val offspring = variation(selectParents())

            population.add(offspring)
        }


        evaluate()

        sortPopulation()

        selectSurvivors()
    }

    override fun evaluate_() : Int {
        return experiment.evaluate2(population)
    }

    /**
     * Rank-based selection of two parents.
     *
     * @return Two parent solutions.
     */
    private fun selectParents(): Pair<SKNet, SKNet> {
        val indices =
            Selection.selectIndices(
                0,
                getAsInt(Parameter.Population_Size) - 1,
                2,
                get(Parameter.Selection_Pressure),
                random
            )

        return Pair(population[indices[0]], population[indices[1]])
    }


    /**
     * Discards the worst |population| - populationSize solutions.
     *
     */
    private fun selectSurvivors() {
        val populationSize = getAsInt(Parameter.Population_Size)

        // Discard the worst solution until the size matches.
        while (population.size > populationSize) {
            population.removeAt(population.size - 1)
        }
    }

    override fun cross(a: SKNet, b: SKNet): SKNet {
        return a.crossWith(
            b, get(Parameter.Prb_Cross_Gene_By_Choosing), nextGenomeID++
        )
    }

    override fun mutate(a: SKNet) {
        a.mutate(
            get(Parameter.Prb_Add_Link),
            get(Parameter.Prb_Add_Neuron),
            get(Parameter.Prb_Remove_Link),
            get(Parameter.Prb_Modify_Weight),
            get(Parameter.Weight_Mutation_Range)
        )
    }


    override fun addXElementsToExport(): List<XElement> {
        return listOf()
    }
}