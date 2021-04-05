package util.latex

/**
 * LaTeX description.
 *
 * @constructor Creates a new instance.
 *
 * @param standalone If true, a document-environment is created around this object, when exported.
 */
open class LatexDescription(standalone: Boolean) : LatexItem(standalone) {
    /**
     * Elements in the description: (name, description).
     */
    val data = mutableListOf<Pair<String, String>>()

    fun addElement(name: String, content: String) {
        data.add(Pair(name, content))
    }

    override fun createSourceCode(): String {
        var result = ""

        if (standalone) {
            result = "\\documentclass[12pt,twoside]{report}\n\n\\begin{document}\n\n\n\n"
        }

        result += "\\begin{description}\n"

        for ((name, content) in data) {
            result += """	\item[\textbf{${escapeLatex(name)}}] \hfill \\
		${escapeLatex(content)}

"""
        }

        result += "\\end{description}\n"

        if (standalone) {
            result += "\n\n\n\n\\end{document}"
        }

        return result
    }
}