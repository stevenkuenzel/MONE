package util.latex

/**
 * A LaTeX object.
 *
 * @property standalone If true, a document-environment is created around this object, when exported.
 * @constructor Creates a new instance.
 */
abstract class LatexItem(val standalone: Boolean) {

    /**
     * Defines a custom LaTeX code.
     */
    private var overwrittenSourceCode = ""

    /**
     * Returns the default LaTeX code.
     */
    abstract fun createSourceCode(): String

    /**
     * Exports this object to LaTeX.
     */
    fun toLaTex(): String {
        return if (overwrittenSourceCode.isNotEmpty()) {
            overwrittenSourceCode
        } else {
            createSourceCode()
        }
    }

    /**
     * Escapes uncerscores (_).
     */
    fun escapeLatex(input: String): String {
        return input.replace("_", "\\_")
    }

    /**
     * Overwrites the LaTeX code.
     *
     * @param sourceCode The new code.
     */
    fun overwriteSourceCode(sourceCode: String) {
        overwrittenSourceCode = sourceCode
    }

    override fun toString(): String {
        return createSourceCode()
    }
}