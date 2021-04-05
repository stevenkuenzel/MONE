package util.latex

/**
 * Table cell.
 */
class Cell(
    var numericContent: Double = Double.NaN,
    var content: String = "",
    val rowSpan: Int = 1,
    val columnSpan: Int = 1,
    var bold: Boolean = false,
    var italic: Boolean = false,
    var lineType: LineType = LineType.Single,
    var sizeModifier: Int = 0
) : LatexItem(false) {
    companion object {
        private val SIZE_STRINGS = arrayOf(
            "\\tiny{",
            "\\scriptsize{",
            "\\footnotesize{",
            "\\small{",
            "\\normalsize{",
            "\\large{",
            "\\Large{",
            "\\LARGE{",
            "\\huge{",
            "\\Huge{"
        )

        private fun modifySize(content: String, sizeModifier: Int): String {
            val sizeModifier_ = if (sizeModifier < -4) -4 else if (sizeModifier > 5) 5 else sizeModifier

            return SIZE_STRINGS[sizeModifier_ + 4] + content + "}"
        }
    }

    var remainsEmpty = false
    var row = -1
    var column = -1
    var rotation = 0
    var colorName = ""

    constructor(lineType: LineType) : this(Double.NaN, "", 1, 1, false, false, lineType, 0)
    {
        remainsEmpty = true
    }

    constructor(sourceCode: String, lineType: LineType) : this(lineType) {
        overwriteSourceCode(sourceCode)
    }


    init {
        if (rowSpan != 1) {
            lineType = LineType.None
        }
    }


    fun setRowAndColumn(row: Int, col: Int) {
        this.row = row
        this.column = col
    }

    override fun createSourceCode(): String {

        var source = content

        if (sizeModifier != 0) {
            source = modifySize(content, sizeModifier)
        }

        if (italic) {
            source = "\\textit{$source}"
        }

        if (bold) {
            source = "\\textbf{$source}"
        }

        if (rotation != 0) {
            source = "\\begin{turn}{$rotation}$source\\end{turn}"
        }

        if (colorName.isNotEmpty()) {
            source = "\\cellcolor{$colorName} $source"
        }

        if (rowSpan > 1) {
            source = "\\multirow{$rowSpan}{*}{$source}"
        }

        if (columnSpan > 1) {
            source = "\\multicolumn{$columnSpan}{|c|}{$source}"
        }

        return source
    }
}