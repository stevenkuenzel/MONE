package util.latex

/**
 * Enum of LaTeX line type for tables.
 *
 * @property latexString The according LaTeX string.
 */
enum class LineType(val latexString : String) {
    None("~"),
    Single("-"),
    Double("="),
}