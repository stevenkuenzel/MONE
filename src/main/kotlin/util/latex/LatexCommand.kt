package util.latex

/**
 * LaTeX command.
 *
 * @constructor Creates a new instance.
 */
class LatexCommand : LatexDescription(false) {

    override fun createSourceCode(): String {
        var result = ""

        for ((name, content) in data) {
            result += """
                \newcommand {${escapeLatex(name)}} {${escapeLatex(content)}}
                
                """.trimIndent()
        }

        return result
    }
}