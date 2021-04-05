package util.latex

/**
 * LaTeX figure element.
 */

/**
 * LaTeX figure.
 *
 * @property imageFile The path to the figure source, e.g. a PDF-file.
 * @constructor Creates a new instance.
 *
 * @param label The label of the object.
 * @param description The description of the object.
 * @param standalone If true, a document-environment is created around this object, when exported.
 */
class LatexFigure(label: String, description: String, standalone: Boolean = false, private val imageFile: String) :
    LabelledLatexItem(label, description, standalone) {
    override fun createSourceCode(): String {
        var result = ""

        if (standalone) {
            result =
                "\\documentclass[12pt,twoside]{report}\n\\usepackage{graphicx}\n\\usepackage[a4paper,width=150mm,top=25mm,bottom=25mm]{geometry}\n\n\\begin{document}\n\n\n\n"
        }

        result +=
            """\begin{figure}[h]
    \centering

    \includegraphics[width=1\linewidth]{{${imageFile}.pdf}}

    ${captionToTex(false)}

    ${labelToTex()}

\end{figure}"""

        if (standalone) {
            result += "\n\n\n\n\\end{document}"
        }

        return result
    }
}