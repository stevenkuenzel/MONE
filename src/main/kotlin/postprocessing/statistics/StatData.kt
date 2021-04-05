package postprocessing.statistics

import org.apache.commons.math3.stat.descriptive.DescriptiveStatistics
import org.apache.commons.math3.stat.ranking.NaNStrategy
import org.apache.commons.math3.stat.ranking.NaturalRanking
import org.apache.commons.math3.stat.ranking.TiesStrategy
import java.util.*

/**
 * Stores the results of an experiment in a K*N-matrix:
 * |t1,   t2, ....,   tK|
 * ----------------------
 * |b1.1, b2.1, .., bK.1|
 * |....................|
 * |....................|
 * |....................|
 * |b1.N, b2.N, .., bK.N|
 *
 * @property numOfTreatments Number of treatments.
 * @property numOfBlocks Maximum number of blocks per treatment.
 * @constructor Creates a new instance.
 */
class StatData(val numOfTreatments: Int, val numOfBlocks: Int) {

    /**
     * Contains the observations and blocks.
     */
    private val data = Array(numOfBlocks) { Array(numOfTreatments) { 0.0 } }

    /**
     * Contains information about which blocks are present or missing.
     */
    private val presenceMatrix = Array(numOfBlocks) { Array(numOfTreatments) { false } }

    /**
     * Copy constructor.
     */
    constructor(parent: StatData) : this(parent.numOfTreatments, parent.numOfBlocks) {
        for (b in data.indices) {
            for (t in data[b].indices) {
                data[b][t] = parent.data[b][t]
                presenceMatrix[b][t] = parent.presenceMatrix[b][t]
            }
        }
    }

    /**
     * Creates a new instance base on a list of observations.
     */
    constructor(input: List<DescriptiveStatistics>) : this(
        input.size,
        input.maxByOrNull { x -> x.values.size }!!.values.size
    ) {
        for (treatment in input.indices) {
            for ((block, value) in input[treatment].values.withIndex()) {
                setValue(treatment, block, value)
            }
        }
    }

    /**
     * Returns the subset of two treatments _t1_ and _t2_.
     *
     * @param t1 Index of the first treatment.
     * @param t2 Index of the second treatment.
     * @return A subset of this instance with two treatments.
     */
    fun getSubset(t1: Int, t2: Int): StatData {
        val copy = StatData(2, numOfBlocks)

        for (b in data.indices) {
            copy.data[b][0] = data[b][t1]
            copy.data[b][1] = data[b][t2]
            copy.presenceMatrix[b][0] = presenceMatrix[b][t1]
            copy.presenceMatrix[b][1] = presenceMatrix[b][t2]
        }

        return copy
    }

    /**
     * Sets the value of a block.
     *
     * @param treatment The index of the treatment.
     * @param block The index of the block.
     * @param value The value of the block.
     */
    fun setValue(treatment: Int, block: Int, value: Double) {
        setValue(treatment, block, value, false)
    }

    /**
     * Sets the value of a block.
     *
     * @param treatment The index of the treatment.
     * @param block The index of the block.
     * @param value The value of the block.
     * @param isVirtual If true, the value is not present but was inserted to fill an empty block.
     */
    fun setValue(treatment: Int, block: Int, value: Double, isVirtual: Boolean) {
        data[block][treatment] = value
        presenceMatrix[block][treatment] = !isVirtual
    }

    /**
     * Returns true, if the given block within the specified treatment is present.
     *
     * @param treatment The index of the treatment.
     * @param block The index of the block.
     * @return True, if the block is present.
     */
    fun isPresent(treatment: Int, block: Int): Boolean {
        return presenceMatrix[block][treatment]
    }

    /**
     * Returns the value of the specified block.
     *
     * @param treatment The index of the treatment.
     * @param block The index of the block.
     * @return The value of the block.
     */
    fun getValue(treatment: Int, block: Int): Double {
        return if (presenceMatrix[block][treatment]) {
            data[block][treatment]
        } else 0.0

    }

    /**
     * Returns true, if no blocks are missing, i.e., all blocks in all treatments are present.
     *
     */
    fun isComplete(): Boolean {
        for (block in presenceMatrix) {
            for (b in block) {
                if (!b) {
                    return false
                }
            }
        }

        return true
    }

    /**
     * Returns true, if no blocks are present, i.e., data is empty.
     */
    fun isEmpty(): Boolean {
        for (block in presenceMatrix) {
            for (b in block) if (b) return false
        }

        return true
    }

    /**
     * Returns the number of treatments.
     */
    fun getK(): Int {
        return numOfTreatments
    }

    /**
     * Returns the maximum number of blocks per treatment.
     */
    fun getN(): Int {
        return numOfBlocks
    }


    /**
     * Returns the number of treatments that have a value present for the specified block.
     *
     * @param block The index of block.
     */
    fun getNumOfTreatmentsInBlock(block: Int): Int {
        assert(0 <= block && block < this.numOfBlocks)

        var present = 0

        for (p in presenceMatrix[block]) {
            if (p) {
                present++
            }
        }

        return present
    }

    /**
     * Returns the min. number of blocks that two treatments _t1_ and _t2_ have in common.
     *
     * @param t1 Index of the first treatment.
     * @param t2 Index of the second treatment.
     * @return
     */
    fun getNumBothTreatmentsInBlock(t1: Int, t2: Int): Int {
        var counter = 0

        for (block in this.presenceMatrix) {
            if (block[t1] && block[t2]) {
                counter++
            }
        }

        return counter
    }

    /**
     * Returns the mean block value of the specified treatment.
     *
     * @param treatment Index of the treatment.
     */
    fun getMeanOfTreatment(treatment: Int): Double {
        var sum = 0.0
        var cnt = 0

        for (i in data.indices) {
            if (presenceMatrix[i][treatment]) {
                sum += data[i][treatment]
                cnt++
            }
        }

        return sum / cnt.toDouble()
    }

    /**
     * Returns a variant of this instance of StatData that contains a row- / block-wise ranking of all treatments' values.
     *
     * @return The ranked StatData instance.
     */
    fun getRanked(): StatData {
        val ranking = NaturalRanking(NaNStrategy.REMOVED, TiesStrategy.AVERAGE)

        var numOfLargeBlocks = 0

        for (i in 0 until numOfBlocks) {
            if (getNumOfTreatmentsInBlock(i) > 1) {
                numOfLargeBlocks++
            }
        }

        val ranked = StatData(this.numOfTreatments, numOfLargeBlocks)

        var targetIndex = 0

        for (i in 0 until numOfBlocks) {
            val presentList = ArrayList<Double>()

            for (j in 0 until numOfTreatments) {
                if (presenceMatrix[i][j]) {
                    presentList.add(this.data[i][j])
                }
            }

            if (presentList.size < 2) {
                continue
            }

            var presentArray = DoubleArray(presentList.size) { j -> presentList[j] }

            var index = 0
            presentArray = ranking.rank(presentArray)

            for (j in this.data[i].indices) {
                if (this.presenceMatrix[i][j]) {
                    ranked.setValue(j, targetIndex, presentArray[index++])
                }
            }

            targetIndex++
        }

        return ranked
    }
}