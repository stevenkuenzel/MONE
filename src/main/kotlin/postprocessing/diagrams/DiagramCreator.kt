package postprocessing.diagrams

import org.jfree.chart.ChartFactory
import org.jfree.chart.JFreeChart
import org.jfree.chart.LegendItemCollection
import org.jfree.chart.axis.NumberAxis
import org.jfree.chart.labels.ItemLabelAnchor
import org.jfree.chart.labels.ItemLabelPosition
import org.jfree.chart.plot.PlotOrientation
import org.jfree.chart.plot.XYPlot
import org.jfree.chart.ui.RectangleInsets
import org.jfree.chart.ui.TextAnchor
import java.awt.Color
import java.awt.Font
import java.text.NumberFormat
import java.util.*

/**
 * Diagram creator for PDF figures.
 *
 * @constructor Creates a new instance.
 */
class DiagramCreator {
    companion object {
        val CHART_WIDTH = 800
        val CHART_HEIGHT = 600
        val PRINT_DIAGRAM_TITLES = false
        val font = Font("Arial Unicode MS", Font.BOLD, 8)

        /**
         * Exports a figure to a PDF.
         */
        private fun writePDF(chart: JFreeChart, width: Int, height: Int, fileName: String) {
            ExtPDFDocumentGraphics2D(chart, fileName, width, height).apply()
        }

        /**
         * Creates a dataset and the corresponding diagram.
         */
        fun createXYDataset(
            yIntervalSeries: List<ExtYIntervalSeries>,
            yMin: Double,
            yMax: Double,
            dataName: String,
            diagramName: String,
            fileName: String
        ) {
            val dataset = ExtXYDataset()
            for (series in yIntervalSeries) {
                dataset.addSeries(series)
            }
            createXYPlot(
                dataset,
                diagramName,
                "Evaluations",
                dataName,
                fileName,
                yMin,
                yMax
            )
        }

        /**
         * Creates a diagram.
         */
        private fun createXYPlot(
            dataset: ExtXYDataset,
            title: String,
            labelX: String,
            labelY: String,
            fileName: String,
            yMin: Double,
            yMax: Double
        ) {
            val chart = ChartFactory.createXYLineChart(
                title,
                labelX, labelY, dataset, PlotOrientation.VERTICAL, true, true, false
            )

            if (!PRINT_DIAGRAM_TITLES) chart.setTitle("")

            val plot = chart.plot as XYPlot
            plot.isDomainPannable = true
            plot.isRangePannable = false
            plot.insets = RectangleInsets(5.0, 5.0, 5.0, 20.0)
            plot.backgroundPaint = Color.white
            plot.rangeAxis.setRange(yMin, yMax)

            val renderer = ExtRenderer(dataset, 3)
            renderer.defaultPositiveItemLabelPosition =
                ItemLabelPosition(ItemLabelAnchor.CENTER, TextAnchor.CENTER, TextAnchor.CENTER, 0.0)
            renderer.defaultItemLabelGenerator = LabelGenerator()
            renderer.defaultItemLabelFont = font
            renderer.defaultItemLabelsVisible = true

            if (dataset.seriesCount < 2) renderer.defaultSeriesVisibleInLegend = false

            dataset.prepare()

            for (i in 0 until dataset.seriesCount) {
                val lineDesign = dataset.getExtSeries(i).lineDesign!!

                renderer.setSeriesPaint(i, lineDesign.colorSet.base)
                renderer.setSeriesShape(i, lineDesign.shape)
                renderer.setSeriesStroke(i, lineDesign.stroke)

                renderer.setSeriesItemLabelPaint(i, lineDesign.colorSet.contrast)
                renderer.setSeriesFillPaint(i, lineDesign.colorSet.tint)
            }

            // Override the legend items.
            if (dataset.seriesCount > 1) {
                val legendItems = LegendItemCollection()

                for (i in 0 until dataset.seriesCount) {
                    if (dataset.getExtSeries(i).showInLegend) {
                        val legendItem = dataset.getExtSeries(i).getLegendItem()!!
                        legendItem.labelFont = Font("Arial Unicode MS", Font.PLAIN, 8)

                        legendItems.add(legendItem)
                    }
                }

                plot.fixedLegendItems = legendItems
            }

            plot.renderer = renderer

            val xAxis = plot.domainAxis as NumberAxis
            xAxis.numberFormatOverride = NumberFormat.getNumberInstance(Locale.ENGLISH)

            val yAxis = plot.rangeAxis as NumberAxis
            yAxis.autoRangeIncludesZero = false
            yAxis.numberFormatOverride = NumberFormat.getNumberInstance(Locale.ENGLISH)

            writePDF(chart, CHART_WIDTH, CHART_HEIGHT, fileName)
        }
    }
}