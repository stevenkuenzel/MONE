package experiments

import elements.IDElement
import elements.genotype.Genotype
import elements.genotype.neuralnetworks.Gene
import elements.genotype.neuralnetworks.NetworkGenotype
import elements.genotype.newannencoding.SKNet
import elements.phenotype.neuralnetworks.NetworkPhenotype
import experiments.conversion.MeanSampleFitness
import experiments.conversion.SampleToFitnessConverter
import experiments.sampling.NoNoiseSampling
import experiments.sampling.SamplingStrategy
import experiments.sampling.StandardErrorDynResampling
import kotlinx.coroutines.*
import settings.SettingManager
import util.events.Event
import util.random.RandomProvider

/**
 * An experiment instance for neural networks.
 *
 * @property numOfInputs Number of input neurons.
 * @property numOfOutputs Number of output neurons.
 * @property bias Do networks forcibly have a bias neuron?
 * @property noise Consider noise during evaluation?
 * @property parallelEvaluation Allow to evaluate multiple solutions in concurrent threds / coroutines.
 * @constructor Creates a new instance of Experiment.
 *
 * @param id The ID of the experiment.
 */
abstract class Experiment(
    id: Int,
    val numOfInputs: Int,
    val numOfOutputs: Int,
    val bias: Boolean,
    val noise: Boolean,
    var parallelEvaluation: Boolean
) : IDElement(id) {
    abstract val name: String
    abstract val evaluateEachSolutionOnce: Boolean

    /**
     * Event that is raised when the evaluation of a solution has been started.
     */
    val onEvaluationStarted = Event<Experiment>()

    /**
     * A random number generator. Has to be set by the EMOA, e.g. NEAT.
     */
    lateinit var random: RandomProvider

    /**
     * A list of references to evaluate solutions against.
     */
    val references = mutableListOf<Reference>()

    /**
     * Determines the strategy to follow to create (one or more) samples during evaluation of a single solution.
     */
    val samplingStrategy: SamplingStrategy = if (noise) StandardErrorDynResampling(
        this,
        SettingManager.global.get("SEDR_SAMPLES_MIN").getValueAsInt(),
        SettingManager.global.get("SEDR_SAMPLES_MAX").getValueAsInt()
    ) else NoNoiseSampling()

    /**
     * Converts one or multiple samples for a solution into a fitness value.
     */
    val sampleToFitnessConverter: SampleToFitnessConverter = MeanSampleFitness()

    /**
     * Progress of the experiment. Has to be set by the EMOA, e.g. NEAT.
     */
    var progress = 0.0

    /**
     * Map of samples for each solution.
     */
    var samples = hashMapOf<Int, SampleData>()

    init {
        // Overwrite the setting concerning concurrent evaluation.
        parallelEvaluation = when (SettingManager.get("EXPERIMENT_EVALUATE_SOLUTIONS_CONCURRENTLY").getValueAsInt()) {
            //0 -> parallelEvaluation // Leave the default value of the experiment.
            1 -> true
            2 -> false
            else -> parallelEvaluation
        }
    }

    /**
     * Evaluates a list of genotypes. These are first converted into phenotypes and then evaluated individually against all references for one or multiple times.
     *
     * @param set The genotypes to evaluate.
     * @return The number of evaluated networks whithin this call of the method.
     */
    fun evaluate(set: List<NetworkGenotype>): Int {
        // Determine the networks that have to be evaluated.
        val toEvaluate = set.filter { !evaluateEachSolutionOnce || it.fitness == null }
        // Create the phenotypes.
        val phenotypes = toEvaluate.map { it.toPhenotype() }

        // Reset all samples and create an empty SampleData for each genotype/phenotype to evaluate.
        samples.clear()
        phenotypes.forEach { samples[it.id] = SampleData(it.id) }

        // Sample the phenotypes as often as allowed.
        if (parallelEvaluation) {
            val scope = CoroutineScope(Dispatchers.Default) // Alternative: newFixedThreadPoolContext

            runBlocking(scope.coroutineContext) {
                for (phenotype in phenotypes) {
                    for (reference in references) {
                        launch {
                            sampleAsOftenAsNecessary(phenotype, reference)
                        }
                    }
                }
            }
        } else {
            for (phenotype in phenotypes) {
                for (reference in references) {
                    sampleAsOftenAsNecessary(phenotype, reference)
                }
            }
        }

        // Convert the samples into fitness vectors and assign those to the genotypes.
        for (networkGenotype in toEvaluate) {
            val sampleData = samples[networkGenotype.id]!!
            val fitnessVector = sampleToFitnessConverter.convert(sampleData)

            networkGenotype.fitness = fitnessVector
        }

        return toEvaluate.size
    }


    /**
     * TODO. EXPERIMENTAL.
     *
     * @param set
     * @return
     */
    fun evaluate2(set: List<SKNet>): Int {
        // Determine the networks that have to be evaluated.
        val toEvaluate = set.filter { !evaluateEachSolutionOnce || it.fitness == null }
        // Create the phenotypes.
        val phenotypes = toEvaluate.map { it.toPhenotype() }

        // Reset all samples and create an empty SampleData for each genotype/phenotype to evaluate.
        samples.clear()
        phenotypes.forEach { samples[it.id] = SampleData(it.id) }

        // Sample the phenotypes as often as allowed.
        if (parallelEvaluation) {
            val scope = CoroutineScope(Dispatchers.Default) // Alternative: newFixedThreadPoolContext

            runBlocking(scope.coroutineContext) {
                for (phenotype in phenotypes) {
                    for (reference in references) {
                        launch {
                            sampleAsOftenAsNecessary(phenotype, reference)
                        }
                    }
                }
            }
        } else {
            for (phenotype in phenotypes) {
                for (reference in references) {
                    sampleAsOftenAsNecessary(phenotype, reference)
                }
            }
        }

        // Convert the samples into fitness vectors and assign those to the genotypes.
        for (networkGenotype in toEvaluate) {
            val sampleData = samples[networkGenotype.id]!!
            val fitnessVector = sampleToFitnessConverter.convert(sampleData)

            networkGenotype.fitness = fitnessVector
        }

        return toEvaluate.size
    }

    /**
     * Evaluates / validates an imported neural network.
     *
     * @param networkPhenotype The network phenotype to evaluate.
     * @param referenceID The ID of the reference to evaluate against.
     */
    abstract fun evaluate(networkPhenotype: NetworkPhenotype, referenceID: Int)

    /**
     * Creates the according number of samples (determined by the sampling strategy) of the phenotype against the reference.
     *
     * @param phenotype The phenotype to sample.
     * @param reference The reference to sample against.
     */
    private fun sampleAsOftenAsNecessary(phenotype: NetworkPhenotype, reference: Reference) {
        if (!samples.containsKey(phenotype.id)) throw Exception("No sample data has been created for phenotype ${phenotype.id}.")

        val sampleData = samples[phenotype.id]!!

        while (samplingStrategy.canSample(sampleData, reference.id)) {
            sampleData.add(sampleAgainst(phenotype, reference))
        }
    }

    /**
     * Create one sample of the network _phenotype_ against the _reference_.
     *
     * @param phenotype The network to sample.
     * @param reference The reference against which the network is sampled.
     * @return The sample containing the revelant information.
     */
    abstract fun sampleAgainst(phenotype: NetworkPhenotype, reference: Reference): SampleVector

    override fun toString(): String {
        return "$id: $name (Noise: $noise)"
    }
}