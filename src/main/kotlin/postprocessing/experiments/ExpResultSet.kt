package postprocessing.experiments

import difference.DiversityMetric
import elements.genotype.Genotype
import org.apache.commons.math3.stat.descriptive.DescriptiveStatistics
import org.apache.commons.math3.util.Precision
import postprocessing.diagrams.DiagramCreator
import postprocessing.diagrams.ExtYIntervalSeries
import postprocessing.statistics.StatData
import postprocessing.statistics.TwoStageTest
import sorting.QProcedure
import sorting.impl.Hypervolume
import util.io.PathUtil
import util.io.ReadWriteUtil
import util.latex.LatexFigure
import util.latex.LatexTable
import util.latex.LineType
import util.latex.StatCase
import java.nio.file.Files
import java.nio.file.Paths
import java.util.regex.Pattern

/**
 * Set of different experiment configurations, e.g. the same experiment approached with different MONAs.
 *
 * @param T The genotype class.
 * @property experiments List of experiments (and their repsective iterations).
 * @property properties User settings.
 * @property qProcedures Quality measures to consider.
 * @constructor Creates a new instance.
 *
 * @param diversityMeasures Diversity measures to consider.
 */
class ExpResultSet<T : Genotype<T>>(
    val experiments: MutableList<ExpResult<T>>,
    val properties: HashMap<String, String>,
    val qProcedures: Array<QProcedure>,
    diversityMeasures: Array<DiversityMetric<T>>
) {
    companion object {
        val COLOR_BEST_VALUE = "green!30"
        val COLOR_WORST_VALUE = "red!30"
    }

    /**
     * Stores inforamtion about a chart.
     */
    data class DiagramInformation(
        val numOfSamples: Int,
        val scale: Pair<Double, Double>?,
        val standardDeviation: Boolean,
        val yName: String,
        val dataName: String
    )

    /**
     * Links an index to a value. Relevant for sorting.
     */
    data class ColumnData(val index: Int, val value: Double)

    /**
     * Short name of the experiment.
     */
    val shortName = if (properties.containsKey("NAME_SHORT")) properties["NAME_SHORT"] else ""

    /**
     * Long name of the experiment.
     */
    val longName = if (properties.containsKey("NAME_LONG")) properties["NAME_LONG"] else ""

    /**
     * Output sub-directory for this experiment set.
     */
    val outputDir = if (shortName!!.isNotEmpty()) "$shortName/" else ""

    val matches = mutableListOf<Pair<Int, Int>>()

    /**
     * The time series to consider in charts.
     */
    val timeSeries: Array<Int>

    val networkSizeNormalized: Boolean
    val outputBinaryString: String


    /**
     * The names occurring in the legend.
     */
    var legendNames = ""

    /**
     * The curve identifiers (numbers) in charts.
     */
    var legendIdentifier = ""

    /**
     * The UUID of the experiment set.
     */
    var uuidHash = -1


    init {
        // Determine the steps in time.
        val steps = mutableListOf<Int>()

        // First step.
        for (experiment in experiments) {
            val ps = experiment.metaInformation["POPULATION_SIZE"]!!.toInt()

            if (!steps.contains(ps)) steps.add(ps)
        }

        // Succeeding steps.
        val numOfSamples = properties["DIAGRAM_SAMPLES"]!!.toInt()

        val stepSize = (experiments[0].metaInformation["EVALUATIONS_MAX"]
            ?: experiments[0].metaInformation["MAX_EVALUATIONS"])!!.toInt() / numOfSamples

        for (i in 0 until numOfSamples) {
            val t = (i + 1) * stepSize

            if (!steps.contains(t)) steps.add(t)
        }

        timeSeries = steps.sorted().toTypedArray()

        // Load properties.
        networkSizeNormalized = properties["NETWORK_SIZE_ONLY_ADDED"]!!.toBoolean()
        outputBinaryString = properties["OUTPUT"]!!

        // Determine the legend.
        val legendPattern = Pattern.compile("\\[.+?\\]")
        val legendMatcher = legendPattern.matcher(properties["LEGEND_NAME_PATTERN"]!!)

        while (legendMatcher.find()) {
            matches.add(Pair(legendMatcher.start(), legendMatcher.end()))
        }

        // Create the output dir, if necessary.
        if (outputDir.isNotEmpty() && !Files.exists(Paths.get(PathUtil.outputDir + outputDir))) {
            Files.createDirectory(Paths.get(PathUtil.outputDir + outputDir))
        }
    }

    /**
     * 2-d Array of experiments.
     */
    val experimentMap = loadExperiments()

    init {
        // Evaluates the experiment set and creates the PDF- and TEX-files.
        val texFiles = mutableListOf<String>()

        texFiles.add(successRate())
        texFiles.add(averageTimeToASolution())
        for (qProcedure in qProcedures) {
            texFiles.add(runtimeBehaviour(qProcedure))
        }
        texFiles.add(meanBestQuality())

        for (diversityMeasure in diversityMeasures) {
            texFiles.add(diversity(diversityMeasure))
        }

        texFiles.add(networkSize(networkSizeNormalized))

        for (qProcedure in qProcedures) {
            texFiles.add(bloat(qProcedure))
        }
//        texFiles.add(meanBestStructure())

        createInputFile(texFiles)
    }


    /**
     * If the setting IDENTIFIER refers to POPULATION_SIZE, replace it by the greek letter My. Leave it unchanged otherwise.
     */
    val translatedIdentifier: String
        get() {
            if (properties["IDENTIFIER"]!! == "POPULATION_SIZE") {
                return "\$\\mu\$"
            }

            return properties["IDENTIFIER"]!!
        }

    /**
     * Decode the MONA, parameter controller, and q-Procedure into their respective strings.
     *
     * @param exp The experiment instance.
     * @param key The setting name.
     * @return The decoded string.
     */
    private fun decodeMetaInformation(exp: ExpResult<T>, key: String): String {
        val value = exp.metaInformation[key] ?: "UNKNOWN"
        val intValue = value.toIntOrNull()

        if (intValue != null) {
            when (key) {
                "ALGORITHM" -> {
                    when (intValue) {
                        0 -> return "nNEAT"
                        1 -> return "NEAT-MODS"
                        2 -> return "NEAT-PS"
                    }
                }
                "PARAMETER_CONTROLLER" -> {
                    when (intValue) {
                        0 -> return "EARPC"
                        1 -> return "DDYPC"
                        2 -> return "Default"
                        3 -> return "Random"

                    }
                }
                "NNEAT_Q_PROCEDURE" -> {
                    when (intValue) {
                        0 -> return "NDR + R2"
                        1 -> return "R2"
                        2 -> return "R2 (it.)"
                        3 -> return "(NDR + R2, CD)"
                    }
                }

                // Do also consider the values from the legacy implementation (on which the experiments in my diss. are based).
                "CONFIGURATION" -> {
                    when (intValue) {
                        0 -> return "Default"
                        1 -> return "Random"
                        3 -> return "EARPC"
                        4 -> return "DDYPC"
                    }
                }
                "QUALITY_MEASURE" -> {
                    when (intValue) {
                        1 -> return "NDR + R2"
                        3 -> return "R2"
                        4 -> return "R2 (it.)"
                        6 -> return "(NDR + R2, CD)"
                    }
                }
            }
        } else {
            return value
        }

        return "UNKNOWN VALUE to decode."
    }

    /**
     * Returns the legend name for a certain configuration.
     *
     * @param exp The experiment instance of the configuration.
     * @param counter The counter for the legend item.
     * @param latex If true, return a LaTeX string.
     * @return The legend name of the configuraiton.
     */
    private fun getLegendName(exp: ExpResult<T>, counter: Int, latex: Boolean): String {
        var result = properties["LEGEND_NAME_PATTERN"]!!

        for (match in matches) {
            val property = properties["LEGEND_NAME_PATTERN"]!!.subSequence(match.first + 1, match.second - 1).toString()

            val value = if (property == "COUNTER") {
                if (latex) {
                    "\$^{\\{$counter\\}}\$"
                } else {
                    "{$counter}"
                }
            } else {
                decodeMetaInformation(exp, property)
            }

            result = result.replace("[$property]", value)
        }

        if (latex) {
            val latexCommand = properties["LEGEND_LATEX_COMMAND"]

            if (latexCommand != null) {
                result = "$latexCommand{$result}"
            }
        }

        return result
    }


    /**
     * Returns true, if the standard deviation is printed in charts.
     */
    private fun considerStandardDeviationInFigures(): Boolean {
        if (!properties.containsKey("STANDARD_DEVIATION")) return false

        return properties["STANDARD_DEVIATION"]!!.toBoolean()
    }

    /**
     * Creates a TEX-file that contains input-references to the files created throughout evaluation. The file itself can be input to a LaTeX document.
     *
     * @param texFiles The list of files to consider.
     */
    private fun createInputFile(texFiles: List<String>) {
        var fileContent = ""

        var ignoreOutputBinaryString = false
        if (outputBinaryString.length != texFiles.size) {
            ignoreOutputBinaryString = true
            println("WARNING: outputBinaryString.length == texFiles.size. Ignoring the defined output.")
        }

        for (index in texFiles.indices) {
            val name = texFiles[index]

            // Ignore null-files.
            if (name.isEmpty()) continue

            val file = "Experiments/$outputDir$name.tex"

            val active = ignoreOutputBinaryString || outputBinaryString[index] == '1'

            fileContent += "${if (active) "" else "%\t"}\\input{$file}\n"
        }

        ReadWriteUtil.writeToFile(outputDir + "Inputfile.tex", fileContent)

        println("\\input{Experiments/${outputDir}Inputfile.tex}")
    }

    /**
     * Loads and sorts the filtered experiments.
     */
    private fun loadExperiments(): Array<Array<ExpResult<T>>> {
        // Determine legend names and sort the experiments.
        experiments.forEach { it.legendName = getLegendName(it, -1, false) }
        experiments.sort()

        // Load the iterations.
        for (experiment in experiments) {
            experiment.load(timeSeries)
        }

        // Sort the experiments according to the user-defined IDENTIFIER. In my thesis this was always POPULATION_SIZE.
        val identifierMap = HashMap<Int, MutableList<ExpResult<T>>>()

        var uuidHashString = ""

        for (experiment in this.experiments) {
            val identifyingAttribute = experiment.metaInformation[properties["IDENTIFIER"]!!]!!.toInt()

            if (!identifierMap.containsKey(identifyingAttribute)) {
                identifierMap[identifyingAttribute] = mutableListOf()
            }

            val experimentData = identifierMap[identifyingAttribute]!!

            experimentData.add(experiment)

            uuidHashString += experiment.uuidHash.toString()
        }

        // Determine the hash of this experiment set.
        uuidHash = uuidHashString.hashCode()

        if (uuidHash < 0) {
            uuidHash += Int.MAX_VALUE
        }

        // Sort the experiments by the identifier (typically POPULATION_SIZE).
        val sortedKeys = identifierMap.keys.sorted()

        val sortedExperiments = Array(sortedKeys.size)
        { i -> Array(identifierMap[sortedKeys[0]]!!.size) { j -> identifierMap[sortedKeys[i]]!![j] } }


        // Create the legend text.
        legendIdentifier = "$translatedIdentifier: "
        legendNames = ""

        for (j in sortedExperiments[0].indices) {
            val exp = sortedExperiments[0][j]

            // Create a string about the configuration (algorithm, q-Procedure, parameter controller) used in the corresponding experiment.
            var algorithmProperties = ""

            val arrayProperties = arrayOf(
                if (exp.metaInformation["ALGORITHM"]!!.toInt() < 2) decodeMetaInformation(
                    exp,
                    "NNEAT_Q_PROCEDURE"
                ) else "",
                decodeMetaInformation(exp, "PARAMETER_CONTROLLER")
            )

            for (property in arrayProperties) {
                if (property.isNotEmpty()) {
                    algorithmProperties += (if (algorithmProperties.isNotEmpty()) ", " else "") + property
                }
            }

            // Update the legend string.
            legendNames += getLegendName(exp, j + 1, true) + " = " + decodeMetaInformation(exp, "ALGORITHM") +
                    (if (algorithmProperties.isNotEmpty()) " ($algorithmProperties)" else "") + "; "
        }


        // Determine the curve identifiers. Only used in charts / figures.
        for (i in sortedExperiments.indices) {
            legendIdentifier += "\\{"

            for (j in sortedExperiments[0].indices) {
                legendIdentifier += (j + i * sortedExperiments[0].size).toString() + ", "
            }

            legendIdentifier =
                legendIdentifier.dropLast(2) + "\\} = " + sortedExperiments[i][0].metaInformation[properties["IDENTIFIER"]!!]!! + "; "
        }

        legendNames = legendNames.dropLast(2)
        legendIdentifier = legendIdentifier.dropLast(2)

        return sortedExperiments
    }

    /**
     * Creates a diagram using JFreeChart.
     */

    /**
     * Creates a diagram using JFreeChart and the referring LaTeX figure.
     *
     * @param getData Lambda expression that returns the data to plot from the experiment configuration.

     * @return A LaTeX figure.
     */
    private fun getDiagram(
        getData: (exp: ExpResult<T>, time: Int) -> DescriptiveStatistics,
        info: DiagramInformation,
        fileName: String
    ): LatexFigure? {

        // Create the series to be printed in the chart.
        val allSeries = mutableListOf<ExtYIntervalSeries>()

        var yMin = Double.MAX_VALUE
        var yMax = Double.NEGATIVE_INFINITY

        for (i in experimentMap.indices) {
            for (j in experimentMap[i].indices) {

                // Create a series (-> curve) for every experiment configuration and population size (or other identifying property).
                val index = i * experimentMap[i].size + j
                val exp = experimentMap[i][j]
                val designData = exp.getDesignData()

                val series = ExtYIntervalSeries(
                    index,
                    getLegendName(exp, j + 1, false),
                    designData[0],
                    designData[1],
                    designData[2],
                    info.standardDeviation
                )

                val populationSize = exp.metaInformation["POPULATION_SIZE"]!!.toInt()

                for (t in timeSeries) {
                    // Ignore entries before first evaluation.
                    if (t < populationSize) continue

                    val ds = getData(exp, t)

                    val meanValue = ds.mean
                    val sdValue = ds.standardDeviation

                    val yValueMax = if (info.standardDeviation) meanValue + sdValue else meanValue
                    val yValueMin = if (info.standardDeviation) meanValue - sdValue else meanValue

                    if (yValueMin < yMin) yMin = yValueMin
                    if (yMax < yValueMax) yMax = yValueMax

                    series.addPoint(t.toDouble(), meanValue, sdValue)
                }

                allSeries.add(series)
            }
        }

        // Determine the min. and max. y-values.
        if (info.scale != null) {
            yMin = info.scale.first
            yMax = info.scale.second
        } else {
            val diffTen = (yMax - yMin) / 10.0
            yMin -= diffTen
            yMax += diffTen
        }

        if (yMin == yMax) {
            // There is no 'interesting' data to plot.
            return null
        }

        // Create a chart as PDF-file.
        DiagramCreator.createXYDataset(allSeries, yMin, yMax, info.yName, info.dataName, outputDir + fileName)

        // Create the LaTeX item that refers to the created chart.
        val figure = LatexFigure("", "", properties["STANDALONE"]!!.toBoolean(), "Experiments/$outputDir$fileName")
        figure.legend = "$legendNames. $legendIdentifier."

        return figure
    }

    /**
     * Creates a LaTeX table.
     *
     * @param getData Lambda expression that returns the data to print in a cell from the experiment configuration.
     * @param getDataName Lambda expression for the name of the data.
     * @param testForStatSignDifferences Conduct a statistical comparison (Skillings-Mack-Test).
     * @param smallerValuesAreBetter True, if a smaller value is preferrable over a larger one.
     * @param lines Variable number of lines to print in each cell, e.g., data and standard deviation.
     * @return A LaTeX table.
     */
    private fun getTable(
        getData: (exp: ExpResult<T>) -> Array<DescriptiveStatistics>,
        getDataName: (exp: ExpResult<T>) -> Array<String>,
        testForStatSignDifferences: Boolean,
        smallerValuesAreBetter: Boolean,
        vararg lines: (exp: ExpResult<T>, ds: DescriptiveStatistics) -> String
    ): LatexTable {

        // Load the data to plot.
        val data = Array(experimentMap.size) { Array(experimentMap[0].size) { Array(0) { DescriptiveStatistics() } } }

        for (i in experimentMap.indices) {
            for (j in experimentMap[i].indices) {
                data[i][j] = getData(experimentMap[i][j])
            }
        }

        /**
         * Some tables are not only 2-dim. (configuration x pop. size), but have a third dimension, e.g. (configuration x pop. size x network topology [neurons, links, recurrent links]).
         */
        val sizeOfThirdDimension = data[0][0].size
        val sizeOfThirdDimensionDescription = (if (sizeOfThirdDimension > 1) 1 else 0)

        val table = LatexTable(
            1 + lines.size * experimentMap.size * sizeOfThirdDimension,
            1 + sizeOfThirdDimensionDescription + experimentMap[0].size,
            "",
            "",
            properties["STANDALONE"]!!.toBoolean()
        )

        // Create headline.
        table.setCell("$\\mu$", 0, 0, 1, 1, true, false, LineType.Double, 0)

        if (sizeOfThirdDimension > 1) {
            table.setCell("", 0, 1, 1, 1, true, false, LineType.Double, 0)
        }

        for (i in experimentMap[0].indices) {
            table.setCell(
                getLegendName(experimentMap[0][i], i + 1, true),
                0,
                1 + sizeOfThirdDimensionDescription + i,
                1,
                1,
                true,
                false,
                LineType.Double,
                0
            )
        }

        // Create left one (or two) row(s).
        val dataNames = getDataName(experimentMap[0][0])

        for (i in experimentMap.indices) {

            table.setCell(
                experimentMap[i][0].metaInformation["POPULATION_SIZE"]!!,
                1 + i * lines.size * sizeOfThirdDimension,
                0,
                lines.size * sizeOfThirdDimension,
                1,
                true,
                false,
                LineType.Single,
                0
            )

            if (sizeOfThirdDimension > 1) {
                for (j in 0 until sizeOfThirdDimension) {
                    table.setCell(
                        dataNames[j],
                        1 + i * lines.size * sizeOfThirdDimension + j * lines.size,
                        1,
                        lines.size,
                        1,
                        true,
                        false,
                        LineType.Single,
                        0
                    )
                }
            }
        }


        /**
         * True, if there is any statistically significant difference among the observations.
         */
        var statSignDiffCheckedAndExisting = false

        // Statistical comparison.
        for (line in data.indices) {
            val statStrings = mutableListOf<Array<String>>()

            if (testForStatSignDifferences) {
                // Do statistical tests and save the results.
                for (rd in 0 until sizeOfThirdDimension) {
                    val input = mutableListOf<DescriptiveStatistics>()

                    for (col in data[0].indices) {
                        input.add(data[line][col][rd])
                    }

                    val stat = TwoStageTest(StatData(input))
                    val testResult = stat.apply()

                    if (!statSignDiffCheckedAndExisting) {
                        statSignDiffCheckedAndExisting = testResult.any { it.isNotEmpty() }
                    }

                    statStrings.add(testResult)
                }
            }

            // Fill the cells.
            for (rd in 0 until sizeOfThirdDimension) {
                val defaultLineIndex = if (testForStatSignDifferences) lines.size - 1 else lines.size

                val dataInRow = mutableListOf<ColumnData>()

                for (col in data[0].indices) {
                    // Obtain the cell information.
                    val descriptiveStatistics = data[line][col][rd]
                    // Convert to the cell value.
                    val valueAsDouble = lines[0](experimentMap[line][col], descriptiveStatistics).toDoubleOrNull()

                    if (valueAsDouble != null) {
                        // If a number, add to the list of values in this row (for color-shading the best and worst value).
                        dataInRow.add(ColumnData(col, valueAsDouble))
                    }

                    // Insert the data in the table.
                    for (index in 0 until defaultLineIndex) {
                        table.setCell(
                            lines[index](experimentMap[line][col], descriptiveStatistics),
                            1 + line * lines.size * sizeOfThirdDimension + rd * lines.size + index,
                            1 + sizeOfThirdDimensionDescription + col,
                            1,
                            1,
                            false,
                            false,
                            if (index == lines.lastIndex) LineType.Single else LineType.None,
                            if (index == 0) 0 else -2
                        )
                    }

                    if (testForStatSignDifferences && statStrings[rd].isNotEmpty()) {
                        // If a statistically significant difference was found, print the corresponding column index.
                        table.setCell(
                            statStrings[rd][col],
                            1 + line * lines.size * sizeOfThirdDimension + rd * lines.size + lines.size - 1,
                            1 + sizeOfThirdDimensionDescription + col,
                            1,
                            1,
                            false,
                            true,
                            LineType.Single,
                            -2
                        )
                    }
                }

                // Determine, whether there are differences among the cells.
                var allEqual = true

                for (i in 1 until dataInRow.size) {
                    if (dataInRow[i - 1].value != dataInRow[i].value) {
                        allEqual = false
                        break
                    }
                }

                // Color-shade the best and worst cells of the row.
                if (!allEqual) {
                    // Find the smallest values.
                    dataInRow.sortByDescending { it.value }
                    var val1 = dataInRow[0].value

                    for (index in dataInRow.indices) {
                        if (index > 0 && dataInRow[index].value != val1) break

                        if (index == dataInRow.size - 1) allEqual = true

                        for (i in lines.indices) {
                            val row = 1 + line * lines.size * sizeOfThirdDimension + rd * lines.size + i
                            val column = 1 + sizeOfThirdDimensionDescription + dataInRow[index].index

                            table.data[row][column].colorName =
                                (if (smallerValuesAreBetter) COLOR_WORST_VALUE else COLOR_BEST_VALUE)
                        }
                    }

                    // Find the largest values.
                    dataInRow.sortBy { it.value }
                    val1 = dataInRow[0].value

                    for (index in dataInRow.indices) {
                        if (index > 0 && dataInRow[index].value != val1) break

                        for (i in lines.indices) {
                            val row = 1 + line * lines.size * sizeOfThirdDimension + rd * lines.size + i
                            val column = 1 + sizeOfThirdDimensionDescription + dataInRow[index].index

                            table.data[row][column].colorName =
                                (if (!smallerValuesAreBetter) COLOR_WORST_VALUE else COLOR_BEST_VALUE)
                        }
                    }
                }
            }
        }

        // Set the information about tests for stat. sign. differences. Relevant to the description of the table.
        table.statCase = when {
            statSignDiffCheckedAndExisting -> {
                StatCase.StatSignDiff
            }
            testForStatSignDifferences -> {
                StatCase.NoStatSignDiff
            }
            else -> {
                StatCase.NoComparison
            }
        }

        // Fill all 'untouched' cells to avoid null pointers.
        table.fillNullCells()

        // Set the legend of the table.
        table.legend = "$legendNames."

        return table
    }


    /**
     * CREATION OF THE SINGLE DIAGRAMS AND TABLES.
     */

    /**
     * Export the runtime behaviour for a given q-Procedure. Figure.
     */
    private fun runtimeBehaviour(qProcedure: QProcedure): String {
        val name = "RTB_${qProcedure.name}"

        val info = DiagramInformation(
            properties["DIAGRAM_SAMPLES"]!!.toInt(),
            null,//Pair(0.0, 1.0),
            considerStandardDeviationInFigures(),
            qProcedure.name,
            "Runtime Behavior"
        )

        val figure = getDiagram(
            { exp: ExpResult<T>, time: Int -> exp.meanQualityAfterTeval(qProcedure, time) },
            info, name
        )!!
        figure.considerStandardDeviation = info.standardDeviation

        figure.description =
            ("(Runtime Behavior) Average ${qProcedure.name} of the Pareto front after the corresponding number of evaluations.")
        figure.shortDescription = ("Runtime Behavior: $longName.")
        figure.label = ("fig:exp:$shortName:RTB_${qProcedure.nameShort}")

        figure.export(outputDir + "${name}_LaTeX")

        return "${name}_LaTeX"
    }

    /**
     * Export the mean genome size. Figure.
     */
    private fun networkSize(normalized: Boolean): String {
        val name = "Size"

        val info = DiagramInformation(
            properties["DIAGRAM_SAMPLES"]!!.toInt(),
            null,
            considerStandardDeviationInFigures(),
            "Average Genome Size",
            "Average Genome Size"
        )

        /**
         * Returns the descriptive statistics about the genome size of the Pareto front for a certain point of time. Postprocesses the information if it has to be normalized, w.r.t. genome size.
         *
         * AUXILIARY METHOD.
         */
        fun postprocessMeanSize(exp: ExpResult<T>, time: Int): DescriptiveStatistics {
            val original = exp.meanSizeAfterTeval(time)
            if (!normalized) return original

            // Get the initial size and subtract that value from all other size values.
            val initialSize = exp.sizeAtTzero()

            val result = DescriptiveStatistics()
            for (value in original.values) {
                result.addValue(value - initialSize)
            }

            // Return the modified instance of DescriptiveStatistics.
            return result
        }

        val figure = getDiagram(
            { exp: ExpResult<T>, time: Int -> postprocessMeanSize(exp, time) },
            info, name
        ) ?: return ""

        figure.considerStandardDeviation = info.standardDeviation;

        figure.description =
            ("Average number of ${if (normalized) "added " else ""}genes (links) of the solutions in the Pareto front after the corresponding number of evaluations.")
        figure.shortDescription = ("Average Number of Genes: $longName.")
        figure.label = ("fig:exp:$shortName:ANNSize")

        figure.export(outputDir + "${name}_LaTeX")

        return "${name}_LaTeX"
    }

    /**
     * Export the bloat. Figure.
     */
    private fun bloat(qProcedure: QProcedure): String {
        val name = "Bloat_${qProcedure.name}"

        val info = DiagramInformation(
            properties["DIAGRAM_SAMPLES"]!!.toInt(),
            null,
            considerStandardDeviationInFigures(),
            "Bloat",
            "Bloat"
        )

        val figure = getDiagram(
            { exp: ExpResult<T>, time: Int -> exp.meanBloatAfterTeval(qProcedure, time) },
            info, name
        )!!
        figure.considerStandardDeviation = info.standardDeviation;

        figure.description =
            ("Average bloat (number of genes / ${qProcedures[0].name}) of the solutions in the Pareto front after the corresponding number of evaluations.")
        figure.shortDescription = ("Average Bloat: $longName.")
        figure.label = ("fig:exp:$shortName:ANNBloat")

        figure.export(outputDir + "${name}_LaTeX")

        return "${name}_LaTeX"
    }


    /**
     * Export the diversity for a given DiversityMeasure. Figure.
     */
    fun diversity(dm: DiversityMetric<T>): String {
        val name = "DIV_${dm.name}"

        val info =
            DiagramInformation(
                properties["DIAGRAM_SAMPLES"]!!.toInt(),
                null,
                considerStandardDeviationInFigures(),
                dm.name,
                "Pareto Front Diversity"
            )

        val figure = getDiagram(
            { exp: ExpResult<T>, time: Int -> exp.meanDiversityAfterTeval(dm, time) },
            info, name
        )!!

        figure.considerStandardDeviation = info.standardDeviation;

        figure.description =
            ("Diversity of the Pareto front (in ${dm.name.toLowerCase()}) after the corresponding number of evaluations. The values provided are relative to the diversity of the initial population.")
        figure.shortDescription = ("Diversity in ${dm.name}: $longName.")
        figure.label = ("fig:exp:$shortName:${dm.nameShort}")

        figure.export(outputDir + "${name}_LaTeX")

        return "${name}_LaTeX"
    }

    /**
     * Export the success rate. Table.
     */
    private fun successRate(): String {
        val outputName = "SuccessRate"

        val table = getTable({ exp: ExpResult<T> -> exp.successRate() },
            { exp: ExpResult<T> -> exp.successRateNames() },
            false,
            false,
            { _: ExpResult<T>, ds: DescriptiveStatistics ->
                Precision.round(ds.values[0] * 100, 0).toInt().toString()
            })// + " \\%" })

        table.description = ("(Success Rate) Partition of successful iterations of the experiment in percent.")
        table.shortDescription = ("Success Rate: $longName.")
        table.label = ("tbl:exp:$shortName:SuccessRate")
//        table.printStatHint = false

        table.export(outputDir + outputName)

        return outputName
    }

    /**
     * Export the average number of evaluations to a solution. Table.
     */
    private fun averageTimeToASolution(): String {
        val outputName = "ATS"


        val table = getTable({ exp: ExpResult<T> -> exp.averageNumberOfEvaluations() },
            { exp: ExpResult<T> -> exp.averageNumberOfEvaluationsNames() },
            true,
            true,
            { exp: ExpResult<T>, ds: DescriptiveStatistics ->
                val sr = ds.values.size.toDouble() / exp.iterations.size.toDouble()
                val str = Precision.round(ds.mean, 0).toInt().toString()

                when {
                    ds.values.isEmpty() -> "-"
                    sr < 0.5 -> "($str)"
                    else -> str
                }
            },
            { exp: ExpResult<T>, ds: DescriptiveStatistics ->
                if (ds.values.isEmpty()) "" else "$\\pm$ " + Precision.round(
                    ds.standardDeviation,
                    0
                ).toInt().toString()
            },
            { exp: ExpResult<T>, ds: DescriptiveStatistics -> "" })

        table.description =
            ("(Average Time to a Solution) Average number of evaluations until a solution of adequate quality had been found. Numbers in brackets result from experiments with success rate \$< 50 \\% \$.")
        table.shortDescription = ("Average Time to a Solution: $longName.")
        table.label = ("tbl:exp:$shortName:ATS")

        table.export(outputDir + outputName)

        return outputName
    }

    private fun meanBestQuality(): String {
        val outputName = "MBQ"

        val table = getTable({ exp: ExpResult<T> ->
            arrayOf(
                exp.meanQualityAfterTeval(
                    Hypervolume().also { it.isEvolutionMode = false },
                    timeSeries.last()
                )
            )
        },
            { exp: ExpResult<T> -> arrayOf("Hypervolume") },
            true,
            false,
            { exp: ExpResult<T>, ds: DescriptiveStatistics -> Precision.round(ds.mean, 3).toString() },
            { exp: ExpResult<T>, ds: DescriptiveStatistics ->
                "$\\pm$ " + Precision.round(ds.standardDeviation, 3).toString()
            },
            { exp: ExpResult<T>, ds: DescriptiveStatistics -> "" })

        table.description = ("(Mean Best Quality) Average Hypervolume dominated by the final Pareto front.")
        table.shortDescription = ("Mean Best Quality: $longName.")
        table.label = ("tbl:exp:$shortName:MBQ")

        table.export(outputDir + outputName)

        return outputName
    }


    /**
     * Export the mean best network-structure. Table.
     */
//    private fun meanBestStructure(): String {
//        val outputName = "MBS"
//
//        val table = getTable({ exp: ExpResult<T> -> exp.meanBestStructure() },
//            { exp: ExpResult<T> -> exp.meanBestStructureNames() },
//            true,
//            true,
//            { exp: ExpResult<T>, ds: DescriptiveStatistics -> Precision.round(ds.mean, 1).toString() },
//            { exp: ExpResult<T>, ds: DescriptiveStatistics ->
//                "$\\pm$ " + Precision.round(ds.standardDeviation, 1).toString()
//            })
//
//        table.description = ("Average topology (quantity) of solutions belonging to the final Pareto front.")
//        table.shortDescription = ("Mean Topology: $longName.")
//        table.label = ("tbl:exp:$shortName:MBS")
//
//        table.export(outputDir + outputName)
//
//        return outputName
//    }
}