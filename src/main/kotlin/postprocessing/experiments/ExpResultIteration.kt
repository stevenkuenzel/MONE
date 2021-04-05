package postprocessing.experiments

import de.stevenkuenzel.xml.XElement
import difference.DiversityMetric
import elements.genotype.Genotype
import elements.innovation.InnovationManager
import org.apache.commons.math3.stat.descriptive.DescriptiveStatistics
import postprocessing.experiments.ExpSettings.Companion.successMinFitnessMap
import postprocessing.experiments.genotypes.ExpGenotypeDecoder
import sorting.ParetoDominance
import sorting.QProcedure
import java.io.File

/**
 * Single iteration of an experiment.
 *
 * @param T The genotype class.
 * @constructor Creates a new instance.
 */
class ExpResultIteration<T : Genotype<T>> private constructor() {

    /**
     * Path to the XML-file.
     */
    var file = ""

    /**
     * Quality measures to consider.
     */
    lateinit var qualityMeasure: Array<QProcedure>

    /**
     * Diversity measures to consider.
     */
    lateinit var diversityMeasures: Array<DiversityMetric<T>>

    /**
     * Size of the population.
     */
    var populationSize = -1

    /**
     * Reference to the instance of the experiment.
     */
    lateinit var experiment: ExpResult<T>

    /**
     * The genotype loader / deserializer.
     */
    var loader: ExpGenotypeDecoder<T>? = null

    companion object {

        /**
         * Loads the iteration of an experiment from an XElement.
         */
        fun <T : Genotype<T>> fromXElement(xElement: XElement, vararg optional: Any): ExpResultIteration<T> {

            val genotypeDecoderBase = optional[0] as ExpGenotypeDecoder<T>
            val result = ExpResultIteration<T>()
            result.loader = genotypeDecoderBase.loadExperimentIteration(xElement)
            result.qualityMeasure = optional[1] as Array<QProcedure>
            result.diversityMeasures = optional[2] as Array<DiversityMetric<T>>
            result.populationSize = optional[3] as Int
            result.experiment = optional[4] as ExpResult<T>
            result.file = File(xElement.filePath).name

            val xSolutions = xElement.getChild("Solutions")!!


            // Ensure compatbility to the legacy format.
            val xGenotypes =
                if (xSolutions.hasChild("Genotype")) xSolutions.getChildren("Genotype") else xSolutions.getChildren("ANN")

            val successMinFitness =
                if (successMinFitnessMap.containsKey(result.experiment.name)) successMinFitnessMap[result.experiment.name]!! else null

            // Load the exported genomes.
            for (xGenotype in xGenotypes) {
                val genotype = result.loader!!.loadGenome(xGenotype)

                // Check, whether the fitness of the genome fulfills the 'success' criterion.
                if (result.networkAESsuccess == null) {
                    if (successMinFitness != null) {
                        var networkDominatesMinFitness = true

                        for (index in genotype.fitness!!.indices) {
                            if (genotype.fitness!![index] > successMinFitness[index]) {
                                networkDominatesMinFitness = false
                                break
                            }
                        }

                        if (networkDominatesMinFitness) {
                            result.networkAESsuccess = genotype
                        }
                    }
                }

                result.solutions[genotype.id] = genotype
                if (result.numOfObjectives == -1) result.numOfObjectives = genotype.fitness!!.size
            }

            return result
        }

    }

    /**
     * The first network that has a fitness equal to or better than the target fitness F*.
     */
    var networkAESsuccess: T? = null

    /**
     * The genomes.
     */
    val solutions = hashMapOf<Int, T>()

    /**
     * The NEAT Innovation manager.
     */
    var innovationManager: InnovationManager? = null

    /**
     * Number of objectives.
     */
    var numOfObjectives: Int = -1

    /**
     * Contains the postprocessed results of the experiment's iterations.
     */
    private val mapData = hashMapOf<Int, ExpInformation<T>>()

    var loaded = false

    /**
     * Loads the data of this iteration for the time series at hand.
     *
     * @param timeSeries The time series.
     */
    fun load(timeSeries: Array<Int>) {
        // Iterations may be reused for multiple experiments. Return, if already postprocessed.
        if (loaded) return

        for (t in timeSeries) {
            if (!isValidTime(t)) continue

            // Determine the Pareto front at time _t_.
            var front = getParetoFrontAfterTeval(t)
            front = ParetoDominance.getNondominated(front, 0)

            if (front.isEmpty()) throw Exception("A front is empty: ${toString()}.")

            // Extract the information.
            mapData[t] = ExpInformation(t, front, this)
        }

        loaded = true

        // Clear the memory.
        solutions.clear()
        innovationManager = null

        println("Evaluated $file")
    }

    /**
     * Do first return data, if the time _t_ is greater or equal to the initial population size.
     */
    private fun isValidTime(t: Int): Boolean {
        return t >= populationSize
    }

    /**
     * Returns the Pareto front at time _t_.
     *
     * @param t The point of time.
     */
    private fun getParetoFrontAfterTeval(t: Int): List<T> {
        return solutions.values.filter { it.id <= t && (it.dominatedAfterEvaluations > t || it.dominatedAfterEvaluations == -1) }
    }

    /**
     * Returns the information about the iteration at time _t_.
     *
     * @param t The point of time.
     */
    fun getInformation(t: Int): ExpInformation<T>? {
        if (!mapData.containsKey(t)) {
            println("No entry for time=$t available.")
            return null
        }

        return mapData[t]!!
    }

    /**
     * Contains the relevant information about the iteration at a certain point of time.
     *
     * @param T The genotype class.
     * @property time The point of time.
     * @property size The average genome size.
     * @property quality The map of quality values.
     * @property diversity The map of diversity values.
     * @property structure The mean structure of the genomes.
     * @property bloat The map of bloat (w.r.t. quality) values.
     * @constructor Creates a new instance.
     */
    class ExpInformation<T : Genotype<T>>(
        val time: Int,
        var size: Double,
        val quality: MutableMap<Int, Double>,
        val diversity: MutableMap<Int, Double>,
        val structure: Array<Double>,
        val bloat: MutableMap<Int, Double>
    ) {
        constructor(time: Int, paretoFront: List<T>, iter: ExpResultIteration<T>) : this(
            time,
            paretoFront.sumBy { it.getGenomeSize() }.toDouble() / paretoFront.size.toDouble(),
            hashMapOf<Int, Double>(),
            hashMapOf<Int, Double>(),
            Array(paretoFront.first().getStructureInformation().size) { 0.0 },
            hashMapOf<Int, Double>()
        ) {
            // Determine the quality and bloat values.
            for (qProcedure in iter.qualityMeasure) {
                quality[qProcedure.key] = qProcedure.computeValue(paretoFront)
                bloat[qProcedure.key] = size / quality[qProcedure.key]!!
            }

            // Determine the diversity values.
            for (diversityMeasure in iter.diversityMeasures) {
                diversity[diversityMeasure.id] = diversityMeasure.getI(diversityMeasure.getData(paretoFront))
//                diversity[diversityMeasure.id] = diversityMeasure.getRiesz(diversityMeasure.getData2(paretoFront))
            }

            // Determine the structure values.
            for (genome in paretoFront) {
                val structureInformation = genome.getStructureInformation()

                for (index in structureInformation.indices) {
                    structure[index] += structureInformation[index]
                }

                for (index in structure.indices) {
                    structure[index] /= paretoFront.size.toDouble()
                }
            }
        }

        /**
         * Returns a copy of this instance.
         */
        fun copy(): ExpInformation<T> {
            return ExpInformation(
                time,
                size,
                quality.toMutableMap(),
                diversity.toMutableMap(),
                structure.copyOf(),
                bloat.toMutableMap()
            )
        }

        /**
         * Returns a mergable copy of this instance.
         */
        fun copyToMergable(): MergedExpInformation<T> {
            val qualityMap = hashMapOf<Int, DescriptiveStatistics>()
            for (elem in quality) {
                qualityMap[elem.key] = DescriptiveStatistics().also { it.addValue(elem.value) }
            }

            val diversityMap = hashMapOf<Int, DescriptiveStatistics>()
            for (elem in diversity) {
                diversityMap[elem.key] = DescriptiveStatistics().also { it.addValue(elem.value) }
            }

            val bloatMap = hashMapOf<Int, DescriptiveStatistics>()
            for (elem in bloat) {
                bloatMap[elem.key] = DescriptiveStatistics().also { it.addValue(elem.value) }
            }

            return MergedExpInformation(
                time,
                DescriptiveStatistics().also { it.addValue(size) },
                qualityMap,
                diversityMap,
                structure.map { value -> DescriptiveStatistics().also { it.addValue(value) } }.toTypedArray(),
                bloatMap
            )
        }
    }

    /**
     * Contains the relevant information about the iteration at a certain point of time. This object can be merged with the ones of other iterations.
     *
     * @param T The genotype class.
     * @property time The point of time.
     * @property size The average genome size.
     * @property quality The map of quality values.
     * @property diversity The map of diversity values.
     * @property structure The mean structure of the genomes.
     * @property bloat The map of bloat (w.r.t. quality) values.
     * @constructor Creates a new instance.
     */
    class MergedExpInformation<T : Genotype<T>>(
        val time: Int,
        var size: DescriptiveStatistics,
        val quality: MutableMap<Int, DescriptiveStatistics>,
        val diversity: MutableMap<Int, DescriptiveStatistics>,
        val structure: Array<DescriptiveStatistics>,
        val bloat: MutableMap<Int, DescriptiveStatistics>
    ) {

        /**
         * Merges this instance with another iteration's one.
         *
         * @param other The other iteration's mergable instance.
         */
        fun merge(other: ExpInformation<T>) {
            if (time != other.time) throw Exception("The time stamps of the samples do not match.")

            size.addValue(other.size)
            for (index in quality.keys) {
                quality[index]!!.addValue(other.quality[index]!!)
            }
            for (index in diversity.keys) {
                diversity[index]!!.addValue(other.diversity[index]!!)
            }
            for (index in bloat.keys) {
                bloat[index]!!.addValue(other.bloat[index]!!)
            }

            for (index in structure.indices) {
                structure[index].addValue(other.structure[index])
            }
        }
    }
}