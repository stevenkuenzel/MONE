package difference

import elements.genotype.Genotype
import sorting.impl.RieszSEnergy

/**
 * Diversity metric based on the moment of inertia.
 *
 * Source: Morrison, Ronald W., and Kenneth A. De Jong. "Measurement of population diversity." International Conference on Artificial Evolution (Evolution Artificielle). Springer, Berlin, Heidelberg, 2001.
 *
 * @param T The genotype class.
 * @constructor Creates a new instance.
 */
abstract class DiversityMetric<T : Genotype<T>> {

    /**
     * First (baseline) value to normalize all succeeding values.
     */
    var baselineValue = 1.0
    var baselineDefined = false

    /**
     * Long name of the metric.
     */
    abstract val name: String

    /**
     * Short name of the metric.
     */
    abstract val nameShort: String

    /**
     * ID of the metric.
     */
    abstract val id: Int

    /**
     * Transforms a list of genotypes into a processable two-dimensional Double array.
     *
     * @param set A list of genotypes.
     * @return Processable Double array.
     */
    abstract fun getData(set: List<T>): Array<Array<Double>>


    /**
     * Returns the neutral element for the according metric.
     *
     * @return The neutral element.
     */
    abstract fun getNeutralElement(): Double

    /**
     * Returns a copy of the metric.
     *
     * @return
     */
    abstract fun copy(): DiversityMetric<T>

    /**
     * Returns the centroid (central element) of a set of vectors.
     *
     * @param data The data to consider.
     * @return The central element.
     */
    private fun getCentroid(data: Array<Array<Double>>): Array<Double> {
        val centroid = Array(data.size) { 0.0 }

        for (i in data.indices) {
            if (data[i].size == 1) {
                centroid[i] = getNeutralElement()
            } else {
                for (v in data[i]) {
                    centroid[i] += v
                }
            }
        }

        for (i in centroid.indices) {
            centroid[i] /= data[i].size.toDouble()
        }

        return centroid
    }

    /**
     * Returns the moment of intertia (= diversity value). Normalized w.r.t. the baseline value.
     *
     * @param data The data to determine the diversity for.
     * @return Moment of inertia.
     */
    fun getI(data: Array<Array<Double>>): Double {
        if (data.isEmpty()) {
            return 0.0
        }

        val centroid = getCentroid(data)

        var I = 0.0

        for (i in data.indices) {
            for (element in data[i]) {
                val diff = element - centroid[i]

                I += diff * diff
            }
        }

        // Scale w.r.t dimension.
        I /= data.size.toDouble()

        if (!baselineDefined) {
            baselineValue = I
            baselineDefined = true

            return 1.0
        }

        return I / baselineValue
    }

    /**
     * Returns the diversity value for the given data.
     *
     * @param set The data to process.
     * @return The diversity value.
     */
    fun get(set: List<T>): Double {
        return getI(getData(set))
    }

    override fun equals(other: Any?): Boolean {
        return other is DiversityMetric<*> && id == other.id
    }

    override fun hashCode(): Int {
        return id
    }

    //    /**
//     * EXPERIMENTAL. Not part of my thesis.
//     *
//     * @param set
//     * @return
//     */
//    abstract fun getData2(set: List<T>): Array<Array<Double>>

//    /**
//     * EXPERIMENTAL. Not part of my thesis.
//     *
//     * @param data The data to process.
//     * @return The diversity.
//     */
//    fun getRiesz(data: Array<Array<Double>>): Double {
//        val rse = RieszSEnergy(1.0, 0.01, null)
//        return rse.computeValue((data))
//    }
}