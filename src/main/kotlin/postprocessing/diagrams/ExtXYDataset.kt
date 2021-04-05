package postprocessing.diagrams

import org.jfree.data.xy.YIntervalSeriesCollection

/**
 * Extends the corresponding class of JFreeChart. Adds further necessary information and function.
 */
class ExtXYDataset : YIntervalSeriesCollection() {
    val seriesSet = mutableListOf<ExtSeriesSet>()

    fun getExtSeries(series: Int): ExtYIntervalSeries {
        return super.getSeries(series) as ExtYIntervalSeries
    }

    private fun getDifferentFeatures(): Array<Boolean> {
        val designIndices = Array(seriesSet.size) { index -> seriesSet[index].representative.designData }
        val differentFeatures = Array(3) { false }


        for (i in 0 until 3) {
            for (j in 1 until designIndices.size) {
                if (designIndices[j][i] != designIndices[0][i]) {
                    differentFeatures[i] = true
                    break
                }
            }
        }

        return differentFeatures
    }

    fun createSeriesSet() {
        val map = hashMapOf<String, ExtSeriesSet>()

        for (i in 0 until seriesCount) {
            val s = getExtSeries(i)

            if (!map.containsKey(s.caption)) {
                map[s.caption] = ExtSeriesSet((s))
            } else {
                map[s.caption]!!.addSeries(s)
            }
        }

        seriesSet.clear()

        for (key in map.keys) {
            seriesSet.add(map[key]!!)
        }
    }

    fun copyFeatures(differentFeatures: Array<Boolean>, data: Array<Int>): Array<Int> {
        val result = Array(3) { 0 }
        val numOfDifferentFeatures = differentFeatures.sumBy { if (it) 1 else 0 }

        if (numOfDifferentFeatures == 1) {
            for (index in differentFeatures.indices) {
                if (differentFeatures[index]) {
                    result[0] = data[index]
                    result[1] = data[index]
                    break
                }
            }
        } else if (numOfDifferentFeatures == 2) {
            var firstIndexSet = false

            for (index in differentFeatures.indices) {
                if (differentFeatures[index]) {
                    if (!firstIndexSet) {
                        result[0] = data[index]
                        firstIndexSet = true
                    } else {
                        result[1] = data[index]
                        result[2] = data[index]

                        break
                    }
                }
            }
        } else {
            return data
        }

        return result
    }

    fun applyLineDesigns() {
        val differentFeatures = getDifferentFeatures()
        val width = (DiagramCreator.CHART_WIDTH - 20 * (seriesCount - 1)).toDouble() / seriesCount.toDouble()

        val elements = mutableListOf<Pair<Array<Int>, Int>>()

        for (index in seriesSet.indices) {
            val designPoints = copyFeatures(differentFeatures, seriesSet.get(index).representative.designData)
            elements.add(Pair(designPoints, index))
        }

        val result = Array(seriesSet.size) { Array(3) { 0 } }

        for (i in 0 until 3)
        {
            elements.sortBy { it.first[i] }

            for (j in 0 until elements.size)
            {
                result[elements[j].second][i] = j
            }
        }

        for (index in seriesSet.indices) {
            seriesSet[index].setLegendDesign(LineDesigner.getLineDesign(result[index]), width)
        }
    }

    fun prepare() {
        if (seriesCount == 0) throw Exception("At least one object of type ExtSeriesSet has to be added before calling prepare.")

        createSeriesSet()

        for (extSeriesSet in seriesSet) {
            extSeriesSet.determineLabel()
        }

        applyLineDesigns()
    }
}