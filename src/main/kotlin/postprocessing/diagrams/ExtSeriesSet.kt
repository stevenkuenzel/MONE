package postprocessing.diagrams


/**
 * Contains one or more interval series and provides the necessary function.
 */
class ExtSeriesSet(val representative: ExtYIntervalSeries) {

    val series = mutableListOf<ExtYIntervalSeries>()

    var label = ""

    init {
        representative.showInLegend = true

        addSeries(representative)
    }

    fun addSeries(series: ExtYIntervalSeries) {
        this.series.add(series)
    }

    fun setLegendDesign(lineDesign: LineDesign, width: Double) {
        for (extYIntervalSeries in series) {
            extYIntervalSeries.applyLineDesign(lineDesign, width)
        }
    }

    fun determineLabel()
    {
        val sortedIDs = series.map { it.id }.sorted().toTypedArray()

        label = representative.caption + " ["

        for (index in sortedIDs.indices) {
            label += sortedIDs[index]

            if (index < sortedIDs.size - 1)
            {
                label += ", "
            }
        }

        label += "]"

        for (extYIntervalSeries in series) {
            extYIntervalSeries.legendLabel = label
        }
    }
}