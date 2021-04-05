package sorting

import elements.FitnessElement
import sorting.impl.NondominatedRanking
import sorting.impl.R2Indicator

/**
 * Combined q-Procedure of two q-Procedures with equal relevance.
 *
 * @property qm1 The first q-Procedure.
 * @property qm2 The second q-Procedure.
 * @constructor Create empty Combined q procedure
 */
class CombinedQProcedure(val qm1: QProcedure, val qm2: QProcedure) : QProcedure(false) {
    override val key: Int
        get() = -1
    override val name: String
        get() = "Combined"
    override val nameShort: String
        get() = "CC"
    override val normalizeDuringEvolution: Boolean
        get() = true

    /**
     * The meta q-Procedure to sort vectors of q-Values.
     */
    val metaQM: QProcedure

    init {
        qm1.previous = this
        qm2.previous = this

        metaQM = NondominatedRanking(R2Indicator())
    }

    override fun <T : FitnessElement> sortImpl(set: MutableList<T>) {
        // Sort the set to determine the respective q-values. Normalize within [0, 1].
        qm1.sortImpl(set)
        qm2.sortImpl(set)
        normalizeAttribute(set, qm1.getAttributeID(), qm1.getAttributeID(), 0.0, 1.0)
        normalizeAttribute(set, qm2.getAttributeID(), qm2.getAttributeID(), 0.0, 1.0)


        // Create meta fitness elements with the two q-values as fitness vector.
        val metaSet = mutableListOf<MetaFitnessElement>()

        for (fitnessElement in set) {
            val metaQF = MetaFitnessElement(fitnessElement)
            metaQF.fitness = arrayOf(1.0 - fitnessElement.getAttribute(qm1.getAttributeID()), 1.0 - fitnessElement.getAttribute(qm2.getAttributeID()))

            metaSet.add(metaQF)
        }

        // Sort the meta fitness elements with the meta sorter. Default: NDR + R2
        metaQM.sort(metaSet)

        // Set original solutions' q-Values to the meta sorter q-values.
        for (metaQFElement in metaSet) {
            metaQFElement.fitnessElement.setAttribute(getAttributeID(), metaQFElement.getAttribute(metaQM.getAttributeID()))
        }

        // Sort the set according to the meta sorter q-values.
        set.sortByDescending { it.getAttribute(getAttributeID()) }
    }

    override fun computeValue(matrix: Array<Array<Double>>): Double {
        // Multiply the q-Values for the fitness vector matrix with each other.
        // However: This method should not be called in practice for a combined q-Procedure.
        var value = 1.0

        value *= qm1.computeValue(matrix)
        value *= qm2.computeValue(matrix)

        return value
    }


    override fun computeContribution(matrix: Array<Array<Double>>): Array<Double> {
        // Only consider the contribution of the first q-Procedure.
        return qm1.computeContribution(matrix)
    }


    /**
     * Stores a fitness element and provides its own fitness vector.
     *
     * @property fitnessElement The fitness element to store (unchanged).
     * @constructor Creates a new instance.
     */
    data class MetaFitnessElement(val fitnessElement: FitnessElement) : FitnessElement(fitnessElement.id)
}