package algorithms

import controlparameters.Parameter
import controlparameters.Parameterized
import controlparameters.controllers.ParameterController
import de.stevenkuenzel.xml.XElement
import elements.genotype.Genotype
import experiments.Experiment
import org.apache.commons.math3.stat.descriptive.DescriptiveStatistics
import org.apache.commons.math3.util.FastMath
import org.apache.commons.math3.util.Precision
import sorting.ParetoDominance
import sorting.QProcedure
import sorting.impl.CrowdingDistance
import util.events.Event
import util.io.PathUtil
import util.random.RandomProvider
import java.util.*

/**
 * Abstract EMOA.
 *
 * @param T The class describing genotypes.
 * @property experiment The experiment instance.
 * @property qProcedure The q-Procedure for sorting.
 * @constructor Creates a new instance of AbstractAlgorithm.
 */
abstract class AbstractAlgorithm<T : Genotype<T>>(val experiment: Experiment, var qProcedure: QProcedure) :
    Parameterized() {

    // Events.
    val onEpochFinished = Event<AbstractAlgorithm<T>>()
    val onEvaluationFinished = Event<AbstractAlgorithm<T>>()
    val onSorted = Event<AbstractAlgorithm<T>>()
    val onPopulationInitialized = Event<AbstractAlgorithm<T>>()
    val onTermination = Event<AbstractAlgorithm<T>>()

    /**
     * Unique id of this algorithm instance.
     */
    val uuid = UUID.randomUUID().toString()

    // The random provider that is applied for all random operations concerning this EMOA.
    val random = RandomProvider.create()


    /**
     * The population, holding all solutions.
     */
    var population = mutableListOf<T>()

    /**
     * Number of finished evaluations.
     */
    var evaluations = 0

    /**
     * Generation number
     */
    var generation = 0

    /**
     * ID of the next solution.
     */
    var nextGenomeID = 0

    /**
     * True, if the EMOA is initialized and ready to use.
     */
    var initialized = false

    /**
     * True, if the EMOA has terminated.
     */
    var terminated = false

    /**
     * True, if a parameter controller has been registered. Note that this is mandatory.
     */
    var parameterControllerRegistered = false


    /**
     * The list of exported phenotype ids. Avoids that a phenotype is exported multiple times.
     */
    private val listOfExportedPhenotypeIDs = mutableListOf<Int>()

    /**
     * The set of solutions (genotypes) to export after termination.
     */
    private val exportSet = hashMapOf<Int, T>()

    /**
     * Holds all solutions that are not dominated by any other solution yet.
     */
    private val nonDominatedYet = mutableListOf<T>()

    /**
     * Registers the relevant paramters for the EMOA to the parameter controller.
     */
    abstract fun registerParameters()

    /**
     * EMOA specific 'run' method. Called by the super-class.
     */
    protected abstract fun run_()

    /**
     * EMOA specific 'epoch' method. Called by the super-class.
     */
    protected abstract fun epoch_()

    /**
     * EMOA specific 'initialize' method. Called by the super-class.
     */
    protected abstract fun initializePopulation_()

    /**
     * EMOA specific 'evaluate' method. Called by the super-class.
     */
    protected abstract fun evaluate_(): Int

    /**
     * Returns true, if the termination condition(s) is/are met. By default it considers the number of evaluations.
     */
    protected open fun terminate_(): Boolean {
        return evaluations >= getAsInt(Parameter.Max_Evaluations)
    }

    /**
     * Sorts the members of the population. By default the members are sorted w.r.t. their repsective q-values.
     */
    protected open fun sortPopulation_() {
        qProcedure.sort(population)
    }

    /**
     * Runs a complete evolutionary cycle.
     */
    fun run() {
        if (!parameterControllerRegistered) throw Exception("No parameter controller has been registered.")

        run_()
    }

    /**
     * Performs an epoch of the EMOA.
     */
    fun epoch() {
        epoch_()


        // Increment the age of all surviving solutions.
        population.forEach { it.age++ }

        generation++
        onEpochFinished(this)
    }

    /**
     * Returns true, if the termination condition(s) is/are met. Calls the respective event.
     */
    protected fun terminate(): Boolean {
        terminated = terminate_()

        if (terminated) onTermination(this)

        return terminated
    }

    /**
     * Creates the initial population. Calls the respective event.
     */
    protected fun initializePopulation() {
        initializePopulation_()

        onPopulationInitialized(this)
    }

    /**
     * Evaluations the members of the population. Calls the respective event.
     */
    protected fun evaluate() {
        evaluations += evaluate_()

        onEvaluationFinished(this)
    }

    /**
     * Sorts the population. Calls the respective event.
     */
    protected fun sortPopulation() {
        sortPopulation_()

        onSorted(this)
    }


    /**
     * Initializes the EMOA. This method has to be called before starting the evolutionary process.
     */
    fun initialize() {
        if (!initialized) {
            registerParameters()
            finalizeParameterRegistration()

            experiment.random = random
            qProcedure.parameterized = this

            onEvaluationFinished += {
                experiment.progress = it.evaluations.toDouble() / it.get(Parameter.Max_Evaluations)
            }


            initialized = true
        }
    }

    /**
     * Registers a parameter controller to the EMOA.
     *
     * @param parameterController The parameter controller.
     * @param qProcedure The q-Procedure of the paramater controller. Only necessary, if the parameter controller operates success-based.
     */
    fun registerParameterController(
        parameterController: ParameterController,
        qProcedure: QProcedure? = CrowdingDistance()
    ) {
        // Set the initial values.
        parameterController.set()

        if (parameterController.requiresUpdate) {
            onEpochFinished += {
                parameterController.updateAndSetValues(it.getPopulationQuality(qProcedure!!))
            }
        }

        parameterControllerRegistered = true
    }

    /**
     * Creates a new solution through variation (recombination and mutation) of two parents.
     *
     * @param parents The parents.
     * @return The child.
     */
    protected fun variation(parents: Pair<T, T?>): T {
        return variation(parents.first, parents.second)
    }

    /**
     * Creates a new solution through variation (recombination and mutation) of two parents.
     *
     * @param a The first parent.
     * @param b The second parent.
     * @return The child.
     */
    protected fun variation(a: T, b: T?): T {
        // Determine whether mutation and/or recombination occur.
        var pMutation = get(Parameter.Prb_Mutation)
        val pCrossover = get(Parameter.Prb_Crossover)

        var mutation: Boolean
        var crossover = false

        if (b == null) {
            pMutation = FastMath.min(pMutation + pCrossover, 1.0)
        } else {
            crossover = random.nextDouble() <= pCrossover
        }

        mutation = random.nextDouble() <= pMutation

        // Enforce that at least one of both variation operation occur to avoid exact copies.
        if (!(crossover || mutation)) {
            if (b != null && random.nextBoolean()) {
                crossover = true
            } else {
                mutation = true
            }
        }

        // Cross the parents or copy the more fit parent.
        val baby = if (crossover) {
            cross(a, b!!)
        } else {
            if (b != null && b.rank < a.rank) b.copy(nextGenomeID++) else a.copy(nextGenomeID++)
        }

        // Mutate the child.
        if (mutation || baby.requiresMutation) {
            mutate(baby)
        }

        // Set meta information.
        baby.experimentID = experiment.id
        baby.generation = generation

        return baby
    }

    /**
     * Recombines two parents into a new solution.
     *
     * @param a The first parent.
     * @param b The second parent.
     * @return The child.
     */
    protected abstract fun cross(a: T, b: T): T

    /**
     * Mutates a solution. Note that the solution itself (and not a copy) is mutated.
     *
     * @param a The solution to mutate.
     */
    protected abstract fun mutate(a: T)


    /**
     * Returns the quality of the currently known Pareto front. Mainly used for parameter control. If no q-Procedure is specified, the default q-Procedure (also taken for sorting the population is considered).
     *
     * Note: In preliminary experiments we found that parameter control benefits from diversity measures (e.g. Crowding Distance) rather than quality measures (e.g. Hypervolume) as quality metric.
     *
     * @param qProcedure The q-Procedure to consider. If left to null, the default is applied
     * @return Quality of the Pareto front.
     */
    fun getPopulationQuality(qProcedure: QProcedure? = null): Double {
        return (qProcedure ?: this.qProcedure).computeValue(getKnownParetoFront())
    }

    /**
     * Returns the known Pareto front based on the current population.
     *
     * @return The known Pareto front.
     */
    private fun getKnownParetoFront(): List<T> {
        return ParetoDominance.getNondominated(population, 0)
    }

    /**
     * Exports the phenotypes generated from the solutions of the known Pareto front to an XML-file each.
     *
     */
    fun exportPhenotypes() {
        val phenotypesOfKnownParetoFront = getKnownParetoFront().map { it.toPhenotype() }

        val namePrefix = "Exp_${experiment.id}_PF_${generation}_ANN_"

        for (phenotype in phenotypesOfKnownParetoFront) {
            if (listOfExportedPhenotypeIDs.contains(phenotype.id)) continue
            listOfExportedPhenotypeIDs.add(phenotype.id)

            val xElement = phenotype.toXElement()
            xElement.addAttribute("ExperimentID", experiment.id)
            xElement.addAttribute("Generation", generation)

            xElement.save("${PathUtil.outputDir}${namePrefix}${phenotype.id}_F${phenotype.genotype.getFitnessAsString()}.xml")
        }
    }

    /**
     * Updates the set of solutions to export after termination. Only solutions that are non-dominated for at least one generation are considered.
     *
     */
    fun updateExportSet() {

        val pf = getKnownParetoFront().sortedBy { it.id }

        for (genotype in nonDominatedYet) {
            for (pfelem in pf) {
                if (genotype.id == pfelem.id) continue

                if (pfelem.dominates(genotype)) {
                    genotype.dominatedAfterEvaluations = pfelem.id
                    break
                }
            }
        }

        nonDominatedYet.removeAll { it.dominatedAfterEvaluations != -1 }

        for (pfelem in pf) {
            if (!nonDominatedYet.contains(pfelem)) nonDominatedYet.add(pfelem)

            if (!exportSet.containsKey(pfelem.id)) {
                exportSet[pfelem.id] = pfelem
            }
        }
    }


    /**
     * Exports all non-dominated (at least one generation) solutions found within the evolutionary process to a single XML-file.
     *
     */
    fun export() {
        val xIteration = XElement("Iteration")
        val xSolutions = xIteration.addChild("Solutions")

        val ids = exportSet.keys.sortedBy { it }

        for (id in ids) {
            xSolutions.addChild(exportSet[id]!!.toXElementWithMetaInformation())
        }

        // Add algorithm (i.e. representation-specific information)
        for (xElement in addXElementsToExport()) {
            xIteration.addChild(xElement)
        }

        xIteration.save(PathUtil.outputDir + "${uuid}.xml")
    }

    abstract fun addXElementsToExport(): List<XElement>

    /**
     * Returns the mean fitness of the population. Values are rounded with the given precision.
     *
     * @param precision The number of decimal places to consider.
     * @return The mean fitness of the population.
     */
    fun getMeanFitness(precision: Int = 3): Array<Double> {
        val ds = Array(population[0].fitness!!.size) { DescriptiveStatistics() }

        for (genome in population) {
            for (i in genome.fitness!!.indices) {
                ds[i].addValue(genome.fitness!![i])
            }
        }

        return Array(ds.size) {i -> Precision.round(ds[i].mean, precision)}
    }
}