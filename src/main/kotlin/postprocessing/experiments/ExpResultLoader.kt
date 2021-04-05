package postprocessing.experiments

import de.stevenkuenzel.xml.XElement
import difference.DiversityMetric
import difference.decisionspace.NetworkDiversity
import difference.objectivespace.FitnessDiversity
import elements.genotype.Genotype
import elements.genotype.neuralnetworks.NetworkGenotype
import elements.genotype.newannencoding.SKNet
import elements.genotype.realvectors.RealVectorGenotype
import postprocessing.experiments.genotypes.ExpGenotypeDecoderNeuralNetwork
import postprocessing.experiments.genotypes.ExpGenotypeDecoderRealVector
import postprocessing.experiments.genotypes.ExpGenotypeDecoderSKNet
import sorting.QProcedure
import sorting.impl.Hypervolume
import sorting.impl.RieszSEnergy
import util.io.PathUtil
import java.io.File
import java.nio.file.Files
import java.util.*
import kotlin.collections.HashMap
import kotlin.streams.toList

/**
 * Finds, filters and composes the experiment files for the defined evaluations (see: 'input/analyse.xml').
 *
 * @constructor Create empty Exp result loader
 */
class ExpResultLoader {
    companion object {
        fun load(xmlFile: String) {
            // Set the q-Procedures to apply.
            val qProcedures: Array<QProcedure> = arrayOf(Hypervolume())
            qProcedures.forEach { it.isEvolutionMode = false }

            // Find all experiment XML-files in the input directory.
            val experimentFiles = findExperimentFiles(PathUtil.inputDir)

            if (experimentFiles.isEmpty()) {
                println("No experiment results found.")
                return
            }

            // Determine the genotype of the experiments. 0 = NEAT-genotype; 1 = SKNet; 2 = Real-vector.
            var genotypeType = 0

            try {
                ExpResult.fromXElement<NetworkGenotype>(
                    XElement.load(experimentFiles.first())!!,
                    ExpGenotypeDecoderNeuralNetwork(),
                    qProcedures,
                    arrayOf<DiversityMetric<NetworkGenotype>>()
                )


            } catch (_: Exception) {
                try {
                    ExpResult.fromXElement<SKNet>(
                        XElement.load(experimentFiles.first())!!,
                        ExpGenotypeDecoderSKNet(),
                        qProcedures,
                        arrayOf<DiversityMetric<SKNet>>()
                    )

                    genotypeType = 1
                } catch (_: Exception) {
                    genotypeType = 2
                }
            }

            // Load the corresponding data.
            when (genotypeType) {
                0 -> {
                    loadNeuralNetwork(xmlFile, experimentFiles, qProcedures)
                }
                1 -> {
                    loadSKNet(xmlFile, experimentFiles, qProcedures)
                }
                else -> {
                    loadRealVector(xmlFile, experimentFiles, qProcedures)
                }
            }
        }

        /**
         * Loads the experiments with neural network representation.
         *
         * @param xmlFile Path to an XML-file that configures the evaluations to conduct.
         * @param experimentFiles The experiment XML-files.
         * @param qualityMeasures The q-Procedures to consider.
         */
        private fun loadNeuralNetwork(
            xmlFile: String,
            experimentFiles: List<String>,
            qualityMeasures: Array<QProcedure>
        ) {
            val genotypeDecoder = ExpGenotypeDecoderNeuralNetwork()

            val experiments = mutableListOf<ExpResult<NetworkGenotype>>()
            val diversityMeasures: Array<DiversityMetric<NetworkGenotype>> =
                arrayOf(NetworkDiversity(), FitnessDiversity())

            for (experimentFile in experimentFiles) {
                experiments.add(
                    ExpResult.fromXElement(
                        XElement.load(experimentFile)!!,
                        genotypeDecoder,
                        qualityMeasures,
                        diversityMeasures
                    )
                )
            }

            if (File(xmlFile).exists()) {
                val sets = filter(experiments, xmlFile)

                sets.forEach {
                    ExpResultSet(it.experiments, it.prop, qualityMeasures, diversityMeasures)
                }
            } else {
                val properties = hashMapOf<String, String>()
                properties["IDENTIFIER"] = "POPULATION_SIZE"
                properties["STANDALONE"] = "true"
                properties["DIAGRAM_SAMPLES"] = "20"
                properties["LEGEND_NAME_PATTERN"] = "[ALGORITHM][COUNTER]"

                ExpResultSet(experiments, properties, qualityMeasures, diversityMeasures)
            }
        }

        /**
         * TODO. EXPERIMENTAL.
         *
         * @param xmlFile Path to an XML-file that configures the evaluations to conduct.
         * @param experimentFiles The experiment XML-files.
         * @param qualityMeasures The q-Procedures to consider.
         */
        private fun loadSKNet(
            xmlFile: String,
            experimentFiles: List<String>,
            qualityMeasures: Array<QProcedure>
        ) {
            val genotypeDecoder = ExpGenotypeDecoderSKNet()

            val experiments = mutableListOf<ExpResult<SKNet>>()
            val diversityMeasures: Array<DiversityMetric<SKNet>> =
                arrayOf(FitnessDiversity())

            for (experimentFile in experimentFiles) {
                experiments.add(
                    ExpResult.fromXElement(
                        XElement.load(experimentFile)!!,
                        genotypeDecoder,
                        qualityMeasures,
                        diversityMeasures
                    )
                )
            }

            if (File(xmlFile).exists()) {
                val sets = filter(experiments, xmlFile)

                sets.forEach {
                    ExpResultSet(it.experiments, it.prop, qualityMeasures, diversityMeasures)
                }
            } else {
                val properties = hashMapOf<String, String>()
                properties["IDENTIFIER"] = "POPULATION_SIZE"
                properties["STANDALONE"] = "true"
                properties["DIAGRAM_SAMPLES"] = "20"
                properties["LEGEND_NAME_PATTERN"] = "[ALGORITHM][COUNTER]"

                ExpResultSet(experiments, properties, qualityMeasures, diversityMeasures)
            }
        }

        /**
         * Loads the experiments with real vector representation.
         *
         * @param xmlFile Path to an XML-file that configures the evaluations to conduct.
         * @param experimentFiles The experiment XML-files.
         * @param qualityMeasures The q-Procedures to consider.
         */
        fun loadRealVector(xmlFile: String, experimentFiles: List<String>, qualityMeasures: Array<QProcedure>) {

            val genotypeDecoder = ExpGenotypeDecoderRealVector()

            val experiments = mutableListOf<ExpResult<RealVectorGenotype>>()
            val diversityMeasures: Array<DiversityMetric<RealVectorGenotype>> = arrayOf(FitnessDiversity())

            for (experimentFile in experimentFiles) {
                experiments.add(
                    ExpResult.fromXElement(
                        XElement.load(experimentFile)!!,
                        genotypeDecoder,
                        qualityMeasures,
                        diversityMeasures
                    )
                )
            }

            if (File(xmlFile).exists()) {
                val sets = filter(experiments, xmlFile)

                sets.forEach {
                    ExpResultSet(it.experiments, it.prop, qualityMeasures, diversityMeasures)
                }
            } else {
                val properties = hashMapOf<String, String>()
                properties["IDENTIFIER"] = "POPULATION_SIZE"
                properties["STANDALONE"] = "true"
                properties["DIAGRAM_SAMPLES"] = "20"
                properties["LEGEND_NAME_PATTERN"] = "[ALGORITHM][COUNTER]"

                ExpResultSet(experiments, properties, qualityMeasures, diversityMeasures)
            }
        }

        /**
         * Links a list of experiments to a map of settings.
         */
        data class ExperimentList<T : Genotype<T>>(
            val experiments: MutableList<ExpResult<T>>,
            val prop: HashMap<String, String>
        )

        /**
         * Filters all experiment XML-files for the matching evaluations described in the file _xmlFile_.
         *
         * @param T The genotype class.
         * @param experiments Experiment instances to filter.
         * @param xmlFile Path to an XML-file that configures the evaluations to conduct.
         * @return The remaining (filtered) experiments per evaluation.
         */
        fun <T : Genotype<T>> filter(experiments: MutableList<ExpResult<T>>, xmlFile: String): List<ExperimentList<T>> {

            val experimentLists = mutableListOf<ExperimentList<T>>()

            val xEvaluations = XElement.load(xmlFile)!!

            // Iterate over all defined evaluations. Multiple evaluations can be carried out in one process.
            for (xEvaluation in xEvaluations.getChildren("Evaluation")) {
                val xConditions = xEvaluation.getChild("Conditions")!!
                val xCondExcl = xConditions.getChild("Exclusive")!!

                var remainingExperiments = experiments.toList()

                // Filter for the exclusive conditions. Matching experiment configurations must have all of these values.
                for (xCondition in xCondExcl.getChildren("Condition")) {
                    val name = xCondition.getAttributeValueAsString("Name")
                    val value = xCondition.getAttributeValueAsString("Value")

                    remainingExperiments = remainingExperiments.filter { it.metaInformation[name] == value }.toList()
                }

                // Filter for the inclusive conditions. Matching experiment configurations must have one of these values.
                val xCondIncl = xConditions.getChild("Inclusive")!!

                val xCondCfg = xCondIncl.getChild("ParameterController") ?: xCondIncl.getChild("Configuration")!!
                val xCondAlg = xCondIncl.getChild("Algorithm")!!
                val xCondQM = xCondIncl.getChild("QProcedure") ?: xCondIncl.getChild("QualityMeasure")!!
                val xCondFurther = xCondIncl.getChild("Further")

                // Each must match at least one of each following group.
                remainingExperiments = remainingExperiments.filter { experiment ->
                    var cfgMatch = false
                    var algMatch = false
                    var qmMatch = false
                    var furtherMatch = xCondFurther == null

                    val cfgID = (experiment.metaInformation["PARAMETER_CONTROLLER"]
                        ?: experiment.metaInformation["CONFIGURATION"]!!).toInt()
                    val algID = experiment.metaInformation["ALGORITHM"]!!.toInt()
                    val qmID = (experiment.metaInformation["NNEAT_Q_PROCEDURE"]
                        ?: experiment.metaInformation["QUALITY_MEASURE"])?.toInt() ?: -1
                    val furtherValue =
                        if (xCondFurther != null) experiment.metaInformation["FURTHER"]!!.toString() else null

                    xCondCfg.children.forEach { (_, v) ->
                        v.forEach { x ->
                            val index = x.getAttributeValueAsInt("Value")

                            if (index == cfgID) {
                                cfgMatch = true
                            }
                        }
                    }

                    xCondAlg.children.forEach { (_, v) ->
                        v.forEach { x ->
                            val index = x.getAttributeValueAsInt("Value")

                            if (index == algID) {
                                algMatch = true
                            }
                        }
                    }

                    if (qmID != -1) {
                        xCondQM.children.forEach { (_, v) ->
                            v.forEach { x ->
                                val index = x.getAttributeValueAsInt("Value")

                                if (index == qmID) {
                                    qmMatch = true
                                }
                            }
                        }
                    } else {
                        // Set match to _true_, if no qProcedure has been defined (NEAT-MODS, NEAT-PS).
                        qmMatch = true
                    }

                    if (xCondFurther != null) {
                        xCondFurther.children.forEach { (_, v) ->
                            v.forEach { x ->
                                val value = x.getAttributeValueAsString("Value")

                                if (value == furtherValue!!) {
                                    furtherMatch = true
                                }
                            }
                        }
                    }

                    cfgMatch && algMatch && qmMatch
                }.toList()

                if (remainingExperiments.isEmpty()) {
                    continue
                }

                // Load the settings for evaluation.
                val settingsMap = hashMapOf<String, String>()
                val xSettings = xEvaluation.getChild("Settings")!!

                xSettings.children.forEach { (_, v) ->
                    v.forEach { x ->
                        settingsMap[x.getAttributeValueAsString("Name")] = x.getAttributeValueAsString("Value")
                    }
                }

                // Add the list of experiments to evaluate in this evaluation.
                experimentLists.add(ExperimentList(remainingExperiments.toMutableList(), settingsMap))
            }

            return experimentLists
        }

        /**
         * Finds all experiment XML-files in the given path.
         *
         * @param path The path to search in (as well as its sub-directories).
         * @return List of all file names / paths.
         */
        private fun findExperimentFiles(path: String): List<String> {
            val result = ArrayList<String>()

            val matchingFiles = Files.walk(File(path).toPath(), Integer.MAX_VALUE).filter { x ->
                val fileName = x.fileName.toString()
                Files.isRegularFile(x) && fileName.startsWith("Experiment") && fileName.endsWith("xml")

            }.toList()

            assert(matchingFiles.isNotEmpty()) { "No matching experiments found." }

            for (file in matchingFiles) {
                result.add(file.toAbsolutePath().toString())
            }

            return result
        }
    }
}