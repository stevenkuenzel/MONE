package postprocessing.experiments

import de.stevenkuenzel.xml.XElement
import de.stevenkuenzel.xml.XLoadable
import difference.DiversityMetric
import elements.genotype.Genotype
import org.apache.commons.math3.stat.descriptive.DescriptiveStatistics
import postprocessing.experiments.genotypes.ExpGenotypeDecoder
import sorting.QProcedure
import java.io.File

/**
 * Contains all iterations, i.e., repetitions of an experiment and evaluates the provided information.
 *
 * @param T The genotype class.
 * @constructor Creates a new instance.
 */
class ExpResult<T : Genotype<T>> private constructor() : Comparable<ExpResult<T>> {
    companion object {
        /**
         * Loads all iterations of the experiment from an XElement.
         */
        fun <T : Genotype<T>> fromXElement(xElement: XElement, vararg optional: Any): ExpResult<T> {
            val file = xElement.filePath
            val parentDirectory = File(file).toPath().parent.toString() + "/"

            val genotypeDecoder = optional[0] as ExpGenotypeDecoder<T>
            val qualityMeasures = optional[1] as Array<QProcedure>
            val diversityMeasures = optional[2] as Array<DiversityMetric<T>>

            val result = ExpResult<T>()

            val xProperties = xElement.getChild("Properties")!!
            val xIterations = xElement.getChild("Iterations")!!

            for (xProperty in xProperties.getChildren("Property")) {
                val name = xProperty.getAttributeValueAsString("Name")
                val value = xProperty.getAttributeValueAsString("Value")

                result.metaInformation[name] = value
            }

            // Load iterations and determine hash.
            var uuidHashString = ""

            for (xIteration in xIterations.getChildren("Iteration")) {
                val iterationFile = xIteration.getAttributeValueAsString("File")

                val diversityMeasuresCopy = Array(diversityMeasures.size) { i -> diversityMeasures[i].copy() }

                result.iterations.add(
                    ExpResultIteration.fromXElement(
                        XElement.load(parentDirectory + iterationFile)!!,
                        genotypeDecoder, qualityMeasures, diversityMeasuresCopy,
                        result.metaInformation["POPULATION_SIZE"]!!.toInt(), result
                    )
                )

                uuidHashString += iterationFile

                println("Loaded $iterationFile")
            }

            result.uuidHash = uuidHashString.hashCode()

            return result
        }
    }

    /**
     * The user settings.
     */
    val metaInformation = hashMapOf<String, String>()

    /**
     * List of iterations.
     */
    val iterations = mutableListOf<ExpResultIteration<T>>()

    /**
     * UUID of the experiment result.
     */
    var uuidHash = -1

    /**
     * Number of objectives.
     */
    var numOfObjectives = -1

    /**
     * Only for sorting. Required for FightingICE case study.
     */
    var legendName = ""


    /**
     * The ID / name of the MONA.
     */
    private val algorithmID: Int
        get() = metaInformation["ALGORITHM"]!!.toInt()

    /**
     * The ID / name of the q-Procedure. nNEAT only.
     */
    private val sortingID: Int
        get() = (metaInformation["NNEAT_Q_PROCEDURE"] ?: metaInformation["QUALITY_MEASURE"])?.toInt() ?: Int.MAX_VALUE

    /**
     * The ID / name of the parameter controller.
     */
    private val configID: Int
        get() = (metaInformation["PARAMETER_CONTROLLER"] ?: metaInformation["CONFIGURATION"])!!.toInt()

    /**
     * The name of the experiment.
     */
    val name: String
        get() = metaInformation["EXPERIMENT"]!!.toString()

    /**
     * Contains the postprocessed results of the experiment's iterations.
     */
    val mapData = hashMapOf<Int, ExpResultIteration.MergedExpInformation<T>>()

    /**
     * Performance measure: Average Time to a Solution of predefined quality.
     */
    var ats = DescriptiveStatistics()

    /**
     * Performance measure: Success Rate.
     */
    var sr = 0.0

    /**
     * Loads the iteration data for the time series at hand.
     *
     * @param timeSeries The time series.
     */
    fun load(timeSeries: Array<Int>) {
        // Load the iterations.
        for (iteration in iterations) {
            iteration.load(timeSeries)
        }

        if (iterations.isEmpty()) throw Exception("Iterations are empty.")

        // Retrieve and merge the information for the according _time_ in the ev. process for all iterations.
        for (time in timeSeries) {
            val data = iterations[0].getInformation(time) ?: continue

            val d = data.copyToMergable()

            for (i in 1 until iterations.size) {
                d.merge(iterations[i].getInformation(time)!!)
            }

            mapData[time] = d
        }

        // Set the success-rate and average-time-to-a-solution values.
        sr = iterations.sumBy { (if (it.networkAESsuccess == null) 0 else 1) }.toDouble() / iterations.size.toDouble()

        for (iteration in iterations) {
            if (iteration.networkAESsuccess != null) ats.addValue(iteration.networkAESsuccess!!.id.toDouble())
        }

        // Set the number of objectives.
        numOfObjectives = iterations.first().numOfObjectives
    }


//    fun meanBestStructure(): Array<DescriptiveStatistics> {
//        val result = Array(3) { DescriptiveStatistics() }
//
//        for (iteration in iterations) {
//            val structure = iteration.getFinalGenomeSize()
//
//            result[0].addValue(structure.neurons)
//            result[1].addValue(structure.links)
//            result[2].addValue(structure.recurrent)
//        }
//
//        return result
//    }

//    fun meanBestStructureNames(): Array<String> {
//        return arrayOf("Neurons", "Links", "Links (Rec)")
//    }

    /**
     * Returns the descriptive statistics about the quality of the Pareto front for a certain point of time and a q-Procedure.
     *
     * @param qProcedure The q-Procedure.
     * @param t The point of time.
     */
    fun meanQualityAfterTeval(qProcedure: QProcedure, t: Int): DescriptiveStatistics {
        val result = DescriptiveStatistics()

        for (iteration in iterations) {
            val data = iteration.getInformation(t) ?: return DescriptiveStatistics()

            result.addValue(data.quality[qProcedure.key]!!)
        }

        return result
    }

//    fun meanFitnessAfterTeval(t: Int, k: Int): DescriptiveStatistics {
//        val result = DescriptiveStatistics()
//
//        for (iteration in iterations) {
//            result.addValue(iteration.getFitnessAfterTeval(t)[k])
//        }
//
//        return result
//    }

    /**
     * Returns the descriptive statistics about the genome size of the Pareto front for a certain point of time.
     *
     * @param t The point of time.
     */
    fun meanSizeAfterTeval(t: Int): DescriptiveStatistics {
        return mapData[t]!!.size
    }

    /**
     * Returns the initial genome size in this experiment.
     *
     * @return
     */
    fun sizeAtTzero(): Double {
        return mapData[mapData.keys.sorted().first()]!!.size.values.first()
    }

    /**
     * Returns the descriptive statistics about the bloat of the Pareto front for a certain point of time and a q-Procedure.
     *
     * @param qProcedure The q-Procedure.
     * @param t The point of time.
     */
    fun meanBloatAfterTeval(qProcedure: QProcedure, t: Int): DescriptiveStatistics {
        return mapData[t]!!.bloat[qProcedure.key]!!
    }

    /**
     * Returns the descriptive statistics about the diversity of the Pareto front for a certain point of time and a diversity measure.
     *
     * @param dm The diversity measure.
     * @param t The point of time.
     */
    fun meanDiversityAfterTeval(dm: DiversityMetric<T>, t: Int): DescriptiveStatistics {
        return mapData[t]!!.diversity[dm.id]!!
    }

    /**
     * Returns the descriptive statistics about the Average Time to a Solution of the experiment. (As a single entry array.)
     *
     */
    fun averageNumberOfEvaluations(): Array<DescriptiveStatistics> {
        return arrayOf(ats)
    }

    /**
     * Returns the column names for the results of the method _averageNumberOfEvaluations_.
     *
     */
    fun averageNumberOfEvaluationsNames(): Array<String> {
        return arrayOf("Success Rate")
    }

    /**
     * Returns the descriptive statistics about the success rate of the experiment. (As a single entry array.)
     *
     */
    fun successRate(): Array<DescriptiveStatistics> {
        return arrayOf(DescriptiveStatistics().also {
            it.addValue(sr)
        })
    }

    /**
     * Returns the column names for the results of the method _successRate_.
     *
     */
    fun successRateNames(): Array<String> {
        return arrayOf("Success Rate")
    }


    /**
     * Returns the design data for that experiment. (Determines the line design in the charts.)
     *
     * @return The design data.
     */
    fun getDesignData(): IntArray {
        return intArrayOf(
            this.metaInformation["ALGORITHM"].hashCode(),
            this.metaInformation["QUALITY_MEASURE"].hashCode(),
            this.metaInformation["CONFIGURATION"].hashCode()
        )
    }

    /**
     * Compares two experiments. Sorting is according to: 1. Algorithm, 2. q-Procedure, 3. Parameter controller, 4. Legend name (only FTG).
     *
     * @param other
     * @return
     */
    override fun compareTo(other: ExpResult<T>): Int {
        val alg1 = algorithmID
        val alg2 = other.algorithmID

        if (alg1 != alg2) {
            return alg1.compareTo(alg2)
        }

        val srt1 = sortingID
        val srt2 = other.sortingID

        if (srt1 != srt2) {
            return srt1.compareTo(srt2)
        }

        val cfg1 = configID
        val cfg2 = other.configID

        if (cfg1 != cfg2) {
            return cfg1.compareTo(cfg2)
        }

        val legend1 = legendName
        val legend2 = other.legendName

        if (legend1 != legend2) {
            return legend1.compareTo(legend2)
        }

        return 0
    }
}