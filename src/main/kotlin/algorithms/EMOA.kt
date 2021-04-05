package algorithms

import controlparameters.Parameter
import de.stevenkuenzel.xml.XElement
import elements.genotype.realvectors.RealVectorGenotype
import experiments.testproblems.TestProblem
import settings.SettingManager
import sorting.QProcedure
import util.Selection

/**
 * The basic framework for an EMOA applied on a test problem. NOT PART OF MY THESIS.
 *
 * @property problem The test problem to solve (e.g. DTLZ1).
 * @constructor Creates a new instance of EMOA.
 *
 * @param qProcedure The q-Procedure for sorting.
 */
class EMOA(val problem: TestProblem, qProcedure: QProcedure) :
    AbstractAlgorithm<RealVectorGenotype>(problem, qProcedure) {

    override fun registerParameters() {
        register(Parameter.Population_Size, 0.0)
        register(Parameter.Max_Evaluations, SettingManager.global.get("EVALUATIONS_MAX").getValueAsDouble())
        register(Parameter.Selection_Pressure)
        register(Parameter.Prb_Mutation)
        register(Parameter.Prb_Crossover)
        register(Parameter.Replacement_Rate)
        register(Parameter.CROSSOVER_SBX_ETA)
        register(Parameter.MUTATION_PBX_ETA)
    }

    override fun run_() {
        initializePopulation()
        evaluate()

        while (!terminate()) {
            epoch()
        }

        updateExportSet()
        export()
    }

    override fun epoch_() {

        updateExportSet()

        val populationSize = getAsInt(Parameter.Population_Size)
        val numOfOffspring =
            (populationSize.toDouble() * get(Parameter.Replacement_Rate)).toInt().coerceIn(1, populationSize)

        for (i in 0 until numOfOffspring) {
            val child = variation(selectParents())
            population.add(child)
        }

        evaluate()

        sortPopulation()

        while (population.size > populationSize) population.removeLast()
    }

    override fun initializePopulation_() {
        val populationSize = getAsInt(Parameter.Population_Size)

        for (i in 0 until populationSize) {
            val genome = Array(problem.numberOfVariables) { random.nextDouble() }
            population.add(RealVectorGenotype(nextGenomeID++, genome, random))
        }
    }

    override fun evaluate_(): Int {
        var evaluated = 0

        for (solution in population) {
            if (solution.fitness != null) continue

            solution.fitness = problem.evaluate(solution.genes)
            evaluated++
        }

        return evaluated
    }

    /**
     * Rank-based selection of two parents.
     *
     * @return
     */
    private fun selectParents(): Pair<RealVectorGenotype, RealVectorGenotype> {
        val indices =
            Selection.selectIndices(0, getAsInt(Parameter.Population_Size) - 1, 2, get(Parameter.Selection_Pressure), random)

        return Pair(population[indices[0]], population[indices[1]])
    }


    override fun cross(a: RealVectorGenotype, b: RealVectorGenotype): RealVectorGenotype {
        return a.crossWith(b, nextGenomeID++, get(Parameter.CROSSOVER_SBX_ETA))
    }

    override fun mutate(a: RealVectorGenotype) {
        a.mutate(get(Parameter.MUTATION_PBX_ETA))
    }

    override fun addXElementsToExport(): List<XElement> {
        return emptyList()
    }
}