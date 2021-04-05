package experiments

import org.apache.commons.math3.stat.descriptive.DescriptiveStatistics
import org.apache.commons.math3.util.FastMath
import java.lang.Exception
import java.util.*

/**
 * Contains all fitness vectors determined for a certain phenotype.
 *
 * @property id The phenotype ID.
 * @constructor Creates a new instance.
 */
data class SampleData(val id : Int) {

    /**
     * Number of objectives.
     */
    var numOfObjectives = -1

    /**
     * Map of samples per reference ID.
     */
    val samples = Collections.synchronizedMap(hashMapOf<Int, MutableList<SampleVector>>())

    /**
     * Clears the map of samples.
     *
     */
    fun clear()
    {
        samples.clear()
    }

    /**
     * Returns the total number of samples (over all references).
     *
     * @return The total number of samples.
     */
    fun size() : Int
    {
        return samples.keys.sumBy { samples[it]!!.size }
    }

    /**
     * Returns the number of samples for a certain reference.
     *
     * @param id The reference ID:
     * @return The number of samples.
     */
    fun size(id : Int) : Int
    {
        return if (known(id)) samples[id]!!.size else 0
    }

    /**
     * Returns true, if no samples are present.
     */
    fun isEmpty() : Boolean
    {
        return samples.isEmpty()
    }

    /**
     * Returns true, if no samples are present concerning the given reference.
     *
     * @param id The reference ID.
     */
    fun isEmpty(id : Int) : Boolean
    {
        return !samples.containsKey(id) || samples[id]!!.isEmpty()
    }

    /**
     * Adds a sample vector to the respective list in the sample map.
     *
     * @param sampleVector The vector to add.
     */
    fun add(sampleVector: SampleVector)
    {
        if (!samples.containsKey(sampleVector.referenceID)) {
            samples[sampleVector.referenceID] = mutableListOf()
        }

        samples[sampleVector.referenceID]!!.add(sampleVector)

        if (numOfObjectives == -1)
        {
            numOfObjectives = sampleVector.objectives.size
        }
    }

    /**
     * Returns true, if at least one sample has been drawn to the according reference.
     *
     * @param id The reference ID.
     */
    fun known(id : Int) : Boolean
    {
        return samples.containsKey(id)
    }

    /**
     * Determines the standard error (applied in moSEDR) for the according reference.
     *
     * @param id The reference ID.
     * @return The standard error.
     */
    fun getStandardError(id : Int) : Double
    {
        if (isEmpty(id)) return 1.0

        var standardError = getSampleStatistics(id)!!.maxByOrNull { it.standardDeviation }!!.standardDeviation
        standardError /= FastMath.sqrt(samples[id]!!.size.toDouble())

        return standardError
    }

    /**
     * Returns an array of DescritiveStatistics instances for all objectives.
     */
    fun getSampleStatistics() : Array<DescriptiveStatistics>
    {
        if (isEmpty())
        {
            throw Exception("Sample data is empty for id $id.")
        }

        val result = Array(numOfObjectives) { DescriptiveStatistics() }

        for (id in samples.keys) {
            val listForID = samples[id]!!

            for (sampleVector in listForID) {
                for (index in sampleVector.objectives.indices) {
                    result[index].addValue(sampleVector.objectives[index])
                }
            }
        }

        return result
    }

    /**
     * Returns an array of DescritiveStatistics instances for all objectives and a certain reference..
     *
     * @param id The reference ID.
     */
    fun getSampleStatistics(id : Int) : Array<DescriptiveStatistics>
    {
        if (isEmpty(id))
        {
            throw Exception("Sample data is empty for id $id.")
        }

        assert(samples.containsKey(id))

        val result = Array(numOfObjectives) { DescriptiveStatistics() }

        for (sampleVector in samples[id]!!) {
            for (k in sampleVector.objectives.indices) {
                result[k].addValue(sampleVector.objectives[k])
            }
        }

        return result
    }
}