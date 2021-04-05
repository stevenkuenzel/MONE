package elements

import sorting.ParetoDominance

/**
 * An element that can get assigned a fitness vector, i.e. solutions or species.
 *
 * @constructor Creates a new instance of FitnessElement.
 *
 * @param id The ID of the element.
 */
open class FitnessElement(id: Int) : IDElement(id), Comparable<FitnessElement> {
    /**
     * The rank of a fitness element within a set of fitness elements (for rank based selection, lower values indicate better elements).
     */
    var rank: Int = -1

    /**
     * The fitness vector.
     */
    var fitness: Array<Double>? = null

    /**
     * The determined final q-value (always normalized within 0 and 1).
     */
    var qValue = 0.0

    /**
     * The generation in which the element has been created. For XML-export and evaluation only.
     */
    var generation: Int = -1

    /**
     * The ID of the experiment instance that created the element. For XML-export and evaluation only.
     */
    var experimentID: Int = -1

    /**
     * The ID of the first element that dominated this element. For XML-export and evaluation only.
     */
    var dominatedAfterEvaluations = -1

    /**
     * Age of the element.
     */
    var age = 0

    /**
     * Stores arbitrary attributes identified by an ID each. Used for determining the q-value.
     */
    var attributes: HashMap<Int, Double> = hashMapOf()

    /**
     * Sets an attribute value.
     *
     * @param id The ID of the attribute.
     * @param value The new value of the attribute.
     */
    fun setAttribute(id: Int, value: Double) {
        attributes[id] = value
    }

    /**
     * Returns the value of an attribute. If the attribute ID is unknown, return -1.
     *
     * @param id The ID of the attribute.
     * @return The value of the attribute.
     */
    fun getAttribute(id: Int): Double {
        return if (attributes.containsKey(id)) attributes[id]!! else -1.0
    }

    /**
     * Crates a new attribute or adds a value to the attribute.
     *
     * @param id The ID of the attribute.
     * @param value The value to set or add.
     */
    fun addAttribute(id: Int, value: Double) {
        if (attributes.containsKey(id)) {
            attributes[id] = attributes[id]!! + value
        } else {
            attributes[id] = value
        }
    }

    override fun compareTo(other: FitnessElement): Int {
        return qValue.compareTo(other.qValue)
    }

    /**
     *Checks whether this instance of FitnessElement dominates another one.
     *
     * @param other The other FitnessElement.
     * @return True, if this instance of FitnessElement determines the _other_ one.
     */
    fun dominates(other: FitnessElement): Boolean {
        if (fitness == null || other.fitness == null || fitness!!.size != other.fitness!!.size) throw Exception("Either fitness array is null or their sizes do not match.")

        return ParetoDominance.dominanceTest(fitness!!, other.fitness!!) == -1
    }
}