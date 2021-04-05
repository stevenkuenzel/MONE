package util.latex

import util.io.ReadWriteUtil

/**
 * A LaTeX object with label and caption.
 *
 * @property label The label of the object.
 * @property description The description of the object.
 * @constructor Creates a new instance.
 *
 * @param standalone If true, a document-environment is created around this object, when exported.
 */
abstract class LabelledLatexItem(var label: String, var description: String, standalone : Boolean) : LatexItem(standalone) {
    companion object
    {
        private const val STAT_HINT_TEXT =
            "Small italic numbers denote statistically significant differences to the value(s) in the corresponding row(s). The $<$ and $>$ signs denote the relation of the corresponding mean ranks."
        private const val STAT_HINT_TEXT_NO_DIFF = "No statistically significant differences occurred."

        private const val SD_HINT_TEXT = "The pale colored area surrounding the curve(s) denotes the standard deviation."
    }

    /**
     * Short description of the object.
     */
    var shortDescription = ""

    /**
     * Legend (appears in the final caption).
     */
    var legend = ""

    /**
     * Have statistical tests been carried out?
     */
    var statCase = StatCase.NoComparison

    /**
     * Show the standard deviation of values?
     */
    var considerStandardDeviation = false

    /**
     * Returns the LaTeX code of the caption.
     *
     * @param printStatSignHint If true, a description about how to read the results of the statistical comparison is added.
     */
    protected open fun captionToTex(printStatSignHint: Boolean): String? {
        var result = "\\caption"

        if (shortDescription.isNotEmpty()) {
            result += "[$shortDescription]"
        }

        result += "{$description ${getStatisticsHint()}${getStandardDeviationHint()}\\scriptsize{$legend}}"

        return result
    }

    /**
     * Returns the LaTeX code of the statistics hint.
     */
    private fun getStatisticsHint(): String {
        return when (statCase) {
            StatCase.NoStatSignDiff -> "\\scriptsize{$STAT_HINT_TEXT_NO_DIFF} "
            StatCase.StatSignDiff -> "\\scriptsize{$STAT_HINT_TEXT} "
            else -> ""
        }
    }

    /**
     * Returns the LaTeX code of the standard deviation hint.
     */
    private fun getStandardDeviationHint(): String {
        return if (considerStandardDeviation) {
            "\\scriptsize{$SD_HINT_TEXT} "
        } else ""
    }


    /**
     * Returns the LaTeX code of the label.
     */
    protected open fun labelToTex(): String? {
        return "\\label{$label}"
    }

    /**
     * Writes the LaTeX code for this object into the file with the given name.
     *
     * @param name The file name.
     * @return The absolute path of the file created.
     */
    open fun export(name: String): String? {
        var name_ = name
        if (!name_.endsWith(".tex")) {
            name_ += ".tex"
        }
        return ReadWriteUtil.writeToFile(name_, toString())
    }


}