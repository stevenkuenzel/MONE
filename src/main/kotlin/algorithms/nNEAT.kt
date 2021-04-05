package algorithms


import controlparameters.Parameter
import elements.genotype.neuralnetworks.NetworkGenotype
import experiments.Experiment
import org.apache.commons.math3.util.Precision
import settings.SettingManager
import sorting.QProcedure
import util.Selection
import kotlin.math.max

/**
 * nNEAT. Application of the framework of SMS-EMOA to evolve neural networks.
 *
 * Source: Steven Kuenzel, and Silja Meyer-Nieberg. "Coping with opponents: multi-objective evolutionary neural networks for fighting games." Neural Computing and Applications (2020): 1-32.
 *
 * @constructor Creates a new instance of nNEAT.
 *
 * @param experiment The experiment instance.
 * @param qProcedure The q-Procedure for sorting.
 */
class nNEAT(experiment: Experiment, qProcedure: QProcedure) : NEAT(experiment, qProcedure) {

    override fun registerParameters() {
        register(Parameter.Population_Size, 0.0)
        register(Parameter.Max_Evaluations, SettingManager.global.get("EVALUATIONS_MAX").getValueAsDouble())
        register(Parameter.Weight_Mutation_Range)
        register(Parameter.Prb_Add_Link)
        register(Parameter.Prb_Add_Neuron)
        register(Parameter.Prb_Modify_Weight)
        register(Parameter.Replacement_Rate)

        register(Parameter.Selection_Pressure)
        register(Parameter.Prb_Mutation)
        register(Parameter.Prb_Crossover)
        register(Parameter.Prb_Cross_Gene_By_Choosing)
        register(Parameter.Prb_Gene_Enabled_On_Crossover)
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

    /**
     * Rank-based selection of two parents.
     *
     * @return Two parent solutions.
     */
    private fun selectParents(): Pair<NetworkGenotype, NetworkGenotype> {
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
}