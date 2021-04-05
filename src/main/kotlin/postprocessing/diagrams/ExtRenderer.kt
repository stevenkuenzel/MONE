package postprocessing.diagrams

import org.jfree.chart.renderer.xy.DeviationRenderer

/**
 * Extends the corresponding class of JFreeChart. Adds further necessary information and function.
 */
class ExtRenderer(private val dataset: ExtXYDataset, private val numOfShapesDepth: Int) : DeviationRenderer(true, true) {
    private val visibleIndices = hashMapOf<Int, MutableList<Int>>()


    override fun getItemShapeVisible(series: Int, item: Int): Boolean {
        return getVisibility(series, item)
    }

    override fun isItemLabelVisible(row: Int, column: Int): Boolean {
        return getVisibility(row, column)
    }

    fun createVisibleIndicesAux(depth: Int, start: Int, end: Int, out: MutableList<Int>) {
        if (depth == 0 || start == end) return

        if (!out.contains(start)) out.add(start)

        if (!out.contains(end)) out.add(end)

        val mid = start + (end - start) / 2

        createVisibleIndicesAux(depth - 1, start, mid, out)
        createVisibleIndicesAux(depth - 1, mid, end, out)
    }

    private fun createVisibleIndices(size: Int): MutableList<Int> {
        val indices = mutableListOf<Int>()

        createVisibleIndicesAux(numOfShapesDepth, 0, size - 1, indices)

        indices.sort()

        return indices
    }

    private fun getVisibleIndices(size: Int): List<Int> {
        if (!visibleIndices.containsKey(size)) {
            visibleIndices[size] = createVisibleIndices(size)
        }

        return visibleIndices[size]!!
    }


    private fun getVisibility(series : Int, item : Int) : Boolean
    {
        val extSeries = dataset.getExtSeries(series)

        val indices = getVisibleIndices(extSeries.itemCount)

        for (index in indices) {
            if (index + extSeries.id == item) return true
        }

        return false
    }
}