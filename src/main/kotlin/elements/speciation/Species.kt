package elements.speciation

import elements.FitnessElement
import elements.genotype.Genotype
import elements.genotype.neuralnetworks.NetworkGenotype
import org.apache.commons.math3.util.FastMath
import util.Selection
import util.random.RandomProvider

class Species<T : Genotype<T>>(id: Int, var representative : T, val random : RandomProvider) : FitnessElement(id) {
    val elements = mutableListOf<T>()

    var generationsWithoutImprovement = 0
    var amountToSpawn = 0

    init {
        elements.add(representative)
    }

    fun add(member : T)
    {
        if (elements.isEmpty())
        {
            representative = member
        }

        elements.add(member)
    }

    fun get(index : Int) : T
    {
        return elements[index]
    }

    fun size() : Int
    {
        return elements.size
    }

    fun clear()
    {
        elements.clear()
    }

    fun selectSingleSolution(selectionPressure : Double) : T
    {
        val numOfReproducible = FastMath.ceil((1.0 - selectionPressure) * this.elements.size.toDouble()).toInt()

        if (numOfReproducible < 2)
        {
            return elements[0]
        }

        return elements[random.nextInt(numOfReproducible)]
    }

    fun selectParents(selectionPressure : Double) : Pair<T, T?>
    {
        val numOfReproducible = FastMath.ceil((1.0 - selectionPressure) * this.elements.size.toDouble()).toInt()

        if (numOfReproducible < 2)
        {
            return Pair(elements[0], null)
        }

        val indices = Selection.selectIndices(0, numOfReproducible - 1, 2, selectionPressure, random)

        if (indices[0] == indices[1])
        {
            return Pair(elements[indices[0]], null)
        }

        return Pair(elements[indices[0]], elements[indices[1]])
    }

    fun canSpawn() : Boolean
    {
        return amountToSpawn-- > 0
    }

}