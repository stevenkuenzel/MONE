package postprocessing.diagrams

import org.jfree.chart.LegendItem
import org.jfree.data.xy.YIntervalSeries
import java.awt.BasicStroke
import java.awt.geom.Line2D
import java.text.AttributedString

/**
 * Extends the corresponding class of JFreeChart. Adds further necessary information and function.
 */
class ExtYIntervalSeries(
    val id: Int,
    val caption: String,
    color: Int,
    shape: Int,
    stroke: Int,
    val standardDeviation: Boolean
) : YIntervalSeries(caption) {

    val designData = createDesignData(color, shape, stroke)

    var showInLegend = false

    var lineDesign: LineDesign? = null
    var legendLineWidth = 0.0

    var legendLabel = ""

    fun applyLineDesign(lineDesign: LineDesign, width: Double) {
        this.lineDesign = lineDesign
        this.legendLineWidth = width
    }

    fun addPoint(x: Double, y: Double, ySD: Double) {
        if (standardDeviation) {
            add(x, y, y - ySD, y + ySD)
        } else {
            add(x, y, y, y)
        }
    }

    fun getLegendItem(): LegendItem? {
        if (lineDesign != null && showInLegend) {
            val attributedString = AttributedString(caption)

            return LegendItem(
                attributedString,
                "",
                "",
                "",
                true,
                lineDesign!!.shape,
                true,
                lineDesign!!.colorSet.base,
                true,
                lineDesign!!.colorSet.base,
                BasicStroke(),
                true,
                Line2D.Double(-legendLineWidth / 2, 0.0, legendLineWidth / 2, 0.0),
                lineDesign!!.stroke,
                lineDesign!!.colorSet.base
            )
        }

        return null
    }

    fun createDesignData(color: Int, shape: Int, stroke: Int): Array<Int> {
        val result = arrayOf(color, shape, stroke)

        for (index in result.indices) {
            if (result[index] < 0) result[index] += Int.MAX_VALUE
        }

        return result
    }

}