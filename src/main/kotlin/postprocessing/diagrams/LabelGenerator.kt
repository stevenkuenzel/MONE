package postprocessing.diagrams

import org.jfree.chart.labels.StandardXYItemLabelGenerator
import org.jfree.data.xy.XYDataset
import postprocessing.diagrams.ExtXYDataset

/**
 * Extends the corresponding class of JFreeChart. Adds further necessary information and function.
 */
class LabelGenerator : StandardXYItemLabelGenerator() {
    override fun generateLabel(dataset: XYDataset?, series: Int, item: Int): String {
        val extXYDataset = dataset as? ExtXYDataset ?: throw Exception("Type of dataset is not ExtXYDataset.")

        return extXYDataset.getExtSeries(series).id.toString()
    }
}