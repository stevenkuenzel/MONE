package thesis.contentgeneration

import controlparameters.Parameter
import org.apache.commons.math3.util.Precision
import util.latex.LatexDescription
import util.latex.LatexTable
import util.latex.LineType

/**
 * Creates LaTeX content about the control parameters for my thesis.
 */
class ParameterLatexInterface {
    companion object {

        /**
         * Prints the content to the console.
         *
         */
        fun printLatexContent()
        {
            println(getCommands())
            println();println();println()
            println(getDescription())
            println();println();println()
            println(getTable())
            println();println();println()
        }

        /**
         * Defines the LaTeX commands.
         */
        fun getCommands(): String {
            var result = ""

            for (parameter in Parameter.values()) {
                result += parameter.toCmd() + "\n"
            }

            return result
        }

        /**
         * Defines the tabular overview over the control parameters and their respective min. and max. values. (Table A.1 in my thesis.)
         *
         */
        fun getTable(): String {
            val parameters = Parameter.values()
            val tbl = LatexTable(
                parameters.size + 1,
                5,
                "tbl:Appendix:Parameters:Default",
                "Control parameters of nNEAT, NEAT-PS and NEAT-MODS. The default values are, if defined, equal to Stanley's definition for Double Pole Balancing with velocities \\cite[p. 148]{stanley2004efficient}. The column \\textit{v=0.5} describes the mean value (Min + Max) / 2.",
                false
            )
            tbl.shortDescription = "Control Parameters of nNEAT."
            tbl.setCell("Parameter", 0, 0, 1, 1, true, false, LineType.Double, 0)
            tbl.setCell("Min", 0, 1, 1, 1, true, false, LineType.Double, 0)
            tbl.setCell("v=0.5", 0, 2, 1, 1, true, false, LineType.Double, 0)
            tbl.setCell("Max", 0, 3, 1, 1, true, false, LineType.Double, 0)
            tbl.setCell("Default", 0, 4, 1, 1, true, false, LineType.Double, 0)

            tbl.setColumnDefinition(0, "|l|")
            tbl.setColumnDefinition(1, "c|")
            tbl.setColumnDefinition(2, "c|")
            tbl.setColumnDefinition(3, "c||")
            tbl.setColumnDefinition(4, "c|")

            tbl.printStatHint = false

            for (index in parameters.indices) {
                val parameter = parameters[index]
                tbl.setCell(parameter.accessByCmd(), index + 1, 0, 1, 1, false, false, LineType.Single, 0)
                tbl.setCell(parameter.min.toString(), index + 1, 1, 1, 1, false, false, LineType.Single, 0)
                tbl.setCell(
                    Precision.round((parameter.min + parameter.max) / 2.0, 3).toString(),
                    index + 1,
                    2,
                    1,
                    1,
                    false,
                    false,
                    LineType.Single,
                    0
                )
                tbl.setCell(parameter.max.toString(), index + 1, 3, 1, 1, false, false, LineType.Single, 0)
                tbl.setCell(parameter.default.toString(), index + 1, 4, 1, 1, false, false, LineType.Single, 0)
            }

            tbl.fillNullCells()

            return tbl.toLaTex()
        }

        /**
         * Defines the description of each control parameter. (Appendix A in my thesis.)
         *
         * @return
         */
        fun getDescription(): String {
            val parameters = Parameter.values()
            val desc = LatexDescription(false)

            for (parameter in parameters) {
                desc.addElement(parameter.accessByCmd(), parameter.description)
            }

            return desc.toLaTex()
        }
    }
}