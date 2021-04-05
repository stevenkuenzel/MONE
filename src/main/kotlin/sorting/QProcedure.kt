package sorting

import controlparameters.Parameterized
import elements.FitnessElement

/**
 * An abstract q-Procedure.
 *
 * @property iterative If true, the q-Procedure runs in iterative mode.
 * @property next The subordinate q-Procedure. Not mandatory.
 * @constructor Creates a new instance.
 */
abstract class QProcedure(val iterative: Boolean, var next: QProcedure? = null) {
    /**
     * Superordinate q-Procedure.
     */
    var previous: QProcedure? = null

    /**
     * Reference to a parameterized object, e.g., the MONA. Allows to access user-settings.
     */
    var parameterized: Parameterized? = null

    /**
     * Unique ID of the q-Procedure instance.
     */
    private val instance = INSTANCE_COUNTER++

    /**
     * Key of the q-Procedure. Relevant for experiment evaluation.
     */
    abstract val key: Int

    /**
     * Name of the q-Procedure.
     */
    abstract val name: String

    /**
     * Short name of the q-Procedure.
     */
    abstract val nameShort: String

    /**
     * Must fitness values be normalized during the evolutionary process? Does not affect the q-Procedure when it is applied in experiment evaluation.
     */
    abstract val normalizeDuringEvolution: Boolean

    /**
     * Flag that determines the current mode (evolution or evaluation).
     */
    var isEvolutionMode = true

    /**
     * Determines the relative margin between two subsets of equal q-values.
     */
    val u = 0.5

    /**
     * Minimum value for normalization. Every normalized value is incremented by this.
     */
    var normMinValue = 0.0

    /**
     * Returns the instance ID of this q-Procedure.
     *
     * @return The instance ID.
     */
    fun getAttributeID(): Int {
        return instance
    }

    init {
        // Build the chain of q-Procedures.
        if (next != null) {
            next!!.previous = this
        }
    }

    /**
     * Determines the q-Value of a complete set of fitness elements.
     *
     * @param set The set of fitness elements, e.g., a population.
     * @return q-Value of the set.
     */
    fun computeValue(set: List<FitnessElement>): Double {
        return computeValue(convertToMatrix(set))
    }

    /**
     * Determines the individual q-Values of all solutions in a set of fitness elements.
     *
     * @param set The set of fitness elements, e.g., a population.
     * @return Array of individual q-Values.
     */
    fun computeContribution(set: List<FitnessElement>): Array<Double> {
        return computeContribution(convertToMatrix(set))
    }

    /**
     * Determines the q-Value of a complete set of fitness elements.
     *
     * @param matrix The matrix of preprocessed fitness vectors.
     * @return q-Value of the set.
     */
    abstract fun computeValue(matrix: Array<Array<Double>>): Double

    /**
     * Determines the individual q-Values of all solutions in a set of fitness elements.
     *
     * @param matrix The matrix of preprocessed fitness vectors.
     * @return Array of individual q-Values.
     */
    abstract fun computeContribution(matrix: Array<Array<Double>>): Array<Double>


    /**
     * Sorts a list of fitness elements. Ensures that all return q-Values are within [0, 1].
     *
     * @param T Type of the fitness elements, e.g., Solution or Species.
     * @param set List of fitness elements.
     */
    fun <T : FitnessElement> sort(set: MutableList<T>) {
        sortImpl(set)

        // If this is the 'super' q-Procedure, normalize within [0, 1].
        if (previous == null) {
            // Normalize final q-value between 0 and 1. Not necessary for sorting.
            normalizeAttribute(set, getAttributeID(), getAttributeID(), 0.0, 1.0)

            for (i in 0 until set.size) {
                set[i].qValue = set[i].getAttribute(getAttributeID())
                set[i].rank = i
            }
        }
    }

    /**
     * Sorts a list of fitness elements.
     *
     * @param T Type of the fitness elements, e.g., Solution or Species.
     * @param set List of fitness elements.
     */
    open fun <T : FitnessElement> sortImpl(set: MutableList<T>) {

        // First, add the attribute of the q-procedure:
        set.forEach { it.setAttribute(getAttributeID(), 0.0) }

        var remaining = set.toMutableList()
        val sorted = mutableListOf<T>()

        while (remaining.size > 1) {
            // Determine all contributions.
            val contributions = computeContribution(remaining)

            val nextRem = mutableListOf<T>()

            var largestContribution = Double.NEGATIVE_INFINITY
            val numOfPreviouslySortedSolutions = sorted.size

            for (i in 0 until remaining.size) {
                val item = remaining[i]

                if (contributions[i].notEqualsDelta(0.0)) {
                    // If the contribution is not equal 0, add the element to the list of sorted solutions.
                    item.setAttribute(getAttributeID(), contributions[i])
                    sorted.add(item)

                    // Find the largest value.
                    if (contributions[i] > largestContribution) {
                        largestContribution = contributions[i]
                    }
                } else if (iterative) {
                    nextRem.add(item)
                }
            }

            // Only for _iterative_ mode: If no new contributing solutions were found, leave the loop.
            if (sorted.size == numOfPreviouslySortedSolutions) {
                break
            }

            // Increment previously sorted "better" solutions by the largest value.
            for (i in 0 until numOfPreviouslySortedSolutions) {
                sorted[i].addAttribute(getAttributeID(), largestContribution)
            }

            remaining = nextRem
        }


        // Do not use the list _sorted_ here, as it may only contain a subset of _set_. This may happen in iterative as well as non-iterative mode.
        // Future Work: This step could be replaced by further book-keeping in the above step.
        set.sortByDescending { x -> x.getAttribute(getAttributeID()) }

        // If a subordinate q-Procedure is defined, search and sort equal contributing solutions.
        if (next != null) {
            val sublists = mutableListOf<SubList<T>>()

            var startIndex = -1

            // Find all sub-lists of solutions with equal q-values.
            for (i in 1 until set.size) {
                if (set[i].getAttribute(getAttributeID()).equalsDelta(set[i - 1].getAttribute(getAttributeID()))) {
                    if (startIndex == -1) {
                        startIndex = i - 1
                    }
                } else {
                    if (startIndex != -1) {
                        val endIndex = i - 1

                        val minMax = getMinMax(startIndex, endIndex, set)
                        sublists.add(
                            SubList(
                                startIndex,
                                endIndex,
                                set.subList(startIndex, endIndex + 1),
                                minMax.first,
                                minMax.second
                            )
                        )

                        startIndex = -1
                    }
                }
            }

            // Consider the last open sub-list.
            if (startIndex != -1) {
                val minMax = getMinMax(startIndex, set.size - 1, set)
                sublists.add(
                    SubList(
                        startIndex,
                        set.size - 1,
                        set.subList(startIndex, set.size),
                        minMax.first,
                        minMax.second
                    )
                )
            }

            // Further sort all sub-lists with _next_.
            if (sublists.isNotEmpty()) {
                for (sublist in sublists) {
                    if (sublist.set.size > 1) {
                        // Sort the equal contributing elements with next QM.
                        next!!.sort(sublist.set)

                        // Normalize in given range.
                        normalizeAttribute(
                            sublist.set,
                            next!!.getAttributeID(),
                            getAttributeID(),
                            sublist.min,
                            sublist.max
                        )
                    }
                }
            }
        }
    }


    /**
     * Converts a list of fitness elements to a matrix of fitness vectors.
     *
     * @param set The list of fitness elements.
     * @return The matrix of fitness vectors.
     */
    private fun convertToMatrix(set: List<FitnessElement>): Array<Array<Double>> {
        val tmp = Array(set.size) { i -> Array(set[0].fitness!!.size) { j -> set[i].fitness!![j] } }

        if (isEvolutionMode && normalizeDuringEvolution) {
            // Important: Normalization is not useful (or even malicious) if:
            // 1. QM is computed for analysis and NOT evolution, OR
            // 2. QM does not profit from normalization. (e.g., NDR -- in contrast to: R2 or HV)

            return normalize(tmp)
        }
        return tmp
    }

    /**
     * Returns a normalized variant of the array provided. Note that Double.NaN is ignored.
     */
    fun normalize(input: Array<Array<Double>>): Array<Array<Double>> {
        val tmp = Array(input.size) { i -> Array(input[0].size) { j -> input[i][j] } }
        val min = Array(tmp[0].size) { Double.MAX_VALUE }
        val max = Array(min.size) { Double.NEGATIVE_INFINITY }

        for (i in tmp.indices) {
            for (j in tmp[i].indices) {
                val value = tmp[i][j]

                if (!value.isNaN()) {
                    if (value < min[j]) min[j] = value
                    if (value > max[j]) max[j] = value
                }
            }
        }

        val diff = Array(min.size) { i -> max[i] - min[i] }

        for (i in tmp.indices) {
            for (j in tmp[i].indices) {
                if (diff[j].notEqualsDelta(0.0)) {
                    if (!tmp[i][j].isNaN()) {
                        val normalizedValue = (tmp[i][j] - min[j]) / diff[j]
                        tmp[i][j] = normMinValue + normalizedValue
                    }
                }
            }
        }

        return tmp
    }

    /**
     * Normalizes the q-Values into the defined range.
     *
     * @param set List of fitness elements.
     * @param sourceID Instance ID of the source q-Procedure (subordinate).
     * @param targetID Instance ID of the target q-procedure (superordinate).
     * @param minT_ Max. q-value to assign.
     * @param maxT_ Min. q-value to assign.
     */
    fun normalizeAttribute(set: List<FitnessElement>, sourceID: Int, targetID: Int, minT_: Double, maxT_: Double) {
        val minT: Double
        val maxT: Double

        // Determine the min. and max. target values.
        if (minT_.equalsDelta(maxT_)) {
            minT = 0.0
            maxT = 1.0
        } else {
            minT = minT_
            maxT = maxT_
        }

        val diffT = maxT - minT

        // Determine the min. and max. source values.
        val minS = set.minByOrNull { x -> x.getAttribute(sourceID) }!!.getAttribute(sourceID)
        val maxS = set.maxByOrNull { x -> x.getAttribute(sourceID) }!!.getAttribute(sourceID)
        val diffS = maxS - minS

        // Change the q-Values accordingly.
        if (diffS.equalsDelta(0.0)) {
            set.forEach { x -> x.setAttribute(targetID, (minT + maxT) / 2.0) }
        } else {
            val diff = diffT / diffS

            set.forEach { x -> x.setAttribute(targetID, minT + (x.getAttribute(sourceID) - minS) * diff) }
        }
    }

    /**
     * Determines the min. and max. q-Values that can by taken by the elements of the sublist [startIndex, endIndex].
     *
     * @param startIndex Index of the first considered element in the list.
     * @param endIndex Index of the last considered element in the list.
     * @param set Complete list of fitness elements.
     * @return Pair of min. and max. value.
     */
    private fun getMinMax(startIndex: Int, endIndex: Int, set: List<FitnessElement>): Pair<Double, Double> {
        val own = set[startIndex].getAttribute(getAttributeID())

        // Determine the next lower / higher values.
        var min = 0.0
        var max = 1.0

        if (endIndex < set.size - 1) {
            min = set[endIndex + 1].getAttribute(getAttributeID())
        }

        if (startIndex > 0) {
            max = set[startIndex - 1].getAttribute(getAttributeID())
        } else if (own > max) {
            max = own
        }

        if (max < min)
        {
            max = min + 1.0
        }

        // Determine the according min. and max. values.
        val dToMin = own - min
        val dToMax = max - own
        val fraction = (1.0 - u) * 0.5

        return Pair(own - dToMin * fraction, own + dToMax * fraction)
    }

    /**
     * Contains information about a sub-list.
     */
    data class SubList<T>(
        val startIndex: Int,
        val endIndex: Int,
        val set: MutableList<T>,
        val min: Double,
        val max: Double
    )
}