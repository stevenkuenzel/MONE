package util.latex

import java.lang.Exception

/**
 * LaTeX table. Supports multirow and multicolumn.
 *
 * @property rows Number of rows.
 * @property columns Number of columns.
 * @constructor Creates a new instance.
 *
 * @param label The label of the object.
 * @param description The description of the object.
 * @param standalone If true, a document-environment is created around this object, when exported.
 */
class LatexTable(val rows: Int, val columns: Int, label: String, description: String, standalone: Boolean) :
    LabelledLatexItem(label, description, standalone) {
    companion object {
        private val COLOR_BEST_VALUE = "green!30"
        private val COLOR_WORST_VALUE = "red!30"
    }

    /**
     * The column definitions.
     */
    private val columnDefinitions = Array(columns) { i -> if (i == 0) "|c|" else "c|" }

    /**
     * The table data.
     */
    val data = Array(rows) { Array(columns) { Cell() } }

    /**
     * Stores information, which cells were not inserted by the user.
     */
    val empty = Array(rows) { Array(columns) { true } }

    /**
     * If true, a description about how to read the results of the statistical comparison is added to the caption.
     */
    var printStatHint = true


    /**
     * Returns a valid column index.
     *
     */
    private fun transformColumnIndex(column: Int): Int {
        return if (column < 0) columns + column else column
    }

    /**
     * Returns a valid row index.
     */
    private fun transformRowIndex(row: Int): Int {
        return if (row < 0) rows + row else row
    }

    /**
     * Updates the definition of a column.
     *
     * @param column The column index.
     * @param definition The new definition.
     */
    fun setColumnDefinition(column: Int, definition: String) {
        columnDefinitions[transformColumnIndex(column)] = definition
    }

    /**
     * Returns the first empty cell (columns-wise iteration).
     *
     * @return Pair of row and column index.
     */
    private fun getNextCell(): Pair<Int, Int>? {
        for (row in 0 until rows) {
            for (col in 0 until columns) {
                if (empty[row][col]) {
                    return Pair(row, col)
                }
            }
        }
        return null
    }

    /**
     * Fills the next empty cell with the given data. Only basic attributes.
     *
     * @param content Cell content as string.
     * @param rowSpan Number of rows. Default = 1.
     * @param columnSpan Number of columns. Default = 1.
     * @param bold Print the content bold?
     * @param italic Print the content italic?
     * @param lineType Line type of the line below the cell.
     */
    fun setCell(content: String, rowSpan: Int, columnSpan: Int, bold: Boolean, italic: Boolean, lineType: LineType?) {
        val (first, second) = getNextCell() ?: return
        setCell(content, first, second, rowSpan, columnSpan, bold, italic, lineType, 0)
    }

    /**
     * Fills a cell with the given data. No rotation.
     *
     * @param numericContent Numeric value of the cell content.
     * @param content Cell content as string.
     * @param row_ Row index.
     * @param column_ Column index.
     * @param rowSpan Number of rows. Default = 1.
     * @param columnSpan Number of columns. Default = 1.
     * @param bold Print the content bold?
     * @param italic Print the content italic?
     * @param lineType Line type of the line below the cell.
     * @param sizeModifier Changes the font size of the cell content. Default = 0. Range of values: [-4, 5].
     */
    fun setCell(
        content: String,
        row: Int,
        column: Int,
        rowSpan: Int,
        columnSpan: Int,
        bold: Boolean,
        italic: Boolean,
        lineType: LineType?,
        sizeModifier: Int
    ) {
        setCell(
            Double.NaN, content, row, column, rowSpan, columnSpan, bold, italic, lineType!!, sizeModifier, 0
        )
    }


    /**
     * Fills a cell with the given data.
     *
     * @param numericContent Numeric value of the cell content.
     * @param content Cell content as string.
     * @param row_ Row index.
     * @param column_ Column index.
     * @param rowSpan Number of rows. Default = 1.
     * @param columnSpan Number of columns. Default = 1.
     * @param bold Print the content bold?
     * @param italic Print the content italic?
     * @param lineType Line type of the line below the cell.
     * @param sizeModifier Changes the font size of the cell content. Default = 0. Range of values: [-4, 5].
     * @param rotation Rotation of the cell in degrees.
     */
    fun setCell(
        numericContent: Double,
        content: String,
        row_: Int,
        column_: Int,
        rowSpan: Int,
        columnSpan: Int,
        bold: Boolean,
        italic: Boolean,
        lineType: LineType,
        sizeModifier: Int,
        rotation: Int
    ) {
        val column = transformColumnIndex(column_)
        val row = transformRowIndex(row_)

        assert(row + (rowSpan - 1) < rows)
        assert(column + (columnSpan - 1) < columns)

        // Create the actual cell.
        val cell = writeCell(
            Cell(
                numericContent, content, rowSpan, columnSpan, bold, italic, lineType, sizeModifier
            ), row, column
        )
        cell.rotation = rotation

        // Create further rows and columns.
        for (i in 1 until rowSpan) {
            writeCell(
                Cell(
                    if (columnSpan > 1) "\\multicolumn{$columnSpan}{c}{}" else "",
                    if (i == rowSpan - 1) lineType else LineType.None
                ), row + i, column
            )
        }

        // The other columns typically remain empty.
        for (i in 1 until columnSpan) {
            writeCell(Cell((if (rowSpan == 1) lineType else LineType.None)), row, column + i)
        }

        // Fill the cells that are still empty. Only apply placeholder cells which do also remain empty in LaTeX.
        for (i in 0 until rowSpan) {
            for (j in 0 until columnSpan) {
                if (empty[row + i][column + j]) {
                    val parent = data[row + i][column]

                    writeCell(Cell(parent.lineType), row + i, column + i)
                }
            }
        }
    }

    /**
     * Adds a cell to the table and removes the corresponding empty marker.
     *
     * @param cell The cell instance to set.
     * @param row The row index.
     * @param column The column index.
     * @return The cell instance.
     */
    private fun writeCell(cell: Cell, row: Int, column: Int): Cell {
        // Add the data.
        data[row][column] = cell
        cell.setRowAndColumn(row, column)

        // Remove the empty-marker.
        empty[row][column] = false

        return cell
    }

    /**
     * Returns the LaTeX code for the column definition of the table.
     *
     */
    private fun createColumnDefinition(): String {
        val result = StringBuilder()

        for (columnDefinition in columnDefinitions) {
            result.append(columnDefinition)
        }

        return result.toString()
    }

    override fun createSourceCode(): String {
        var result = StringBuilder()

        if (standalone) {
            result =
                StringBuilder("\\documentclass[12pt,twoside]{report}\n\\usepackage{multirow}\n\\usepackage[table]{xcolor}\n\\usepackage{hhline}\n\\usepackage{rotating}\n\\usepackage[a4paper,width=150mm,top=25mm,bottom=25mm]{geometry}\n\n\\begin{document}\n\n\n\n")
        }

        result.append("\\begin{table}[h]\n\t\\centering\n\n\t").append(captionToTex(printStatHint)).append("\n\n\t")
            .append(labelToTex()).append("\n\n\t\\begin{tabular}{").append(createColumnDefinition())
            .append("}\n\t\t\\hline\n")

        for (row in 0 until rows) {
            result.append("\t")
            var hhlineString = "|"

            for (col in 0 until columns) {
                if (empty[row][col]) {
                    throw Exception("ERROR: Cell $row, $col is null.")
                }

                val cell = data[row][col]

                if (!cell.remainsEmpty) {
                    result.append("\t").append(cell.toLaTex()).append("\t&")
                }
                hhlineString += cell.lineType.latexString + "|"
            }
            result = StringBuilder(
                """${result.substring(0, result.length - 1)}\\ \hhline{$hhlineString}
"""
            )
        }

        result.append("\t\\end{tabular}\n\n\t").append("\n\\end{table}")

        if (standalone) {
            result.append("\n\n\n\n\\end{document}")
        }

        return result.toString()
    }

    /**
     * Fills all cells with an empty string that have not been inserted yet.
     *
     */
    fun fillNullCells() {
        for (row in 0 until rows) {
            for (col in 0 until columns) {
                if (empty[row][col]) {
                    writeCell(Cell("", LineType.Single), row, col)
                }
            }
        }
    }

    /**
     * Changes the color of the corresponding cell.
     *
     * @param row The row index.
     * @param column The column index.
     * @param isBest If true, the cell is marked as best. Otherwise as worst.
     */
    fun setCellColor(row: Int, column: Int, isBest: Boolean) {
        data[row][column].colorName = if (isBest) COLOR_BEST_VALUE else COLOR_WORST_VALUE
    }
}