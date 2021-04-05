package elements.innovation

import util.random.RandomProvider
import de.stevenkuenzel.xml.XLoadable
import de.stevenkuenzel.xml.XElement
import de.stevenkuenzel.xml.XSavable

/**
 * The innovation manager, based on Stanley's work.
 *
 * Source: Stanley, Kenneth Owen. Efficient evolution of neural networks through complexification. Diss. 2004.
 *
 * @property random A random number generator.
 * @constructor Creates a new instance of InnovationManager.
 *
 * @param numOfInput Number of input neurons of all neural networks for the problem at hand.
 * @param numOfOutput Number of output neurons of all neural networks for the problem at hand.
 * @param bias Create a bias neuron?
 */
class InnovationManager(numOfInput: Int, numOfOutput: Int, bias: Boolean, val random: RandomProvider) : XSavable {

    companion object : XLoadable<InnovationManager> {
        /**
         * Only for loading an innovation manager from disk.
         */
        override fun fromXElement(xElement: XElement, vararg optional: Any): InnovationManager {
            val innovationManager = InnovationManager(-1, -1, false, RandomProvider.create())

            val xInnovations = xElement.getChild("Innovations")!!
            val xNeurons = xElement.getChild("Neurons")!!

            xInnovations.getChildren("Innovation").forEach {
                val newInnovation = Innovation.fromXElement(it)

                innovationManager.innovations.add(newInnovation)
                innovationManager.innovationMap[newInnovation.innovationID] = newInnovation
            }

            xNeurons.getChildren("NeuronInfo").forEach {
                val newNeuronInfo = NeuronInfo.fromXElement(it)

                innovationManager.neuronInfoMap[newNeuronInfo.id] = newNeuronInfo
            }

            return innovationManager
        }

    }

    /**
     * ID of the next innovation.
     */
    var nextInnovationID = 0

    /**
     * ID of the next neuron.
     */
    var nextNeuronID = 0

    /**
     * List of all innovations created.
     */
    private val innovations = arrayListOf<Innovation>()

    /**
     * Maps the innovation ID to the according Innovation instance.
     */
    private val innovationMap = hashMapOf<Int, Innovation>()

    /**
     * Stores meta information about the neuron with the according ID.
     */
    val neuronInfoMap = hashMapOf<Int, NeuronInfo>()


    /**
     * Creates the initial data of the InnovationManager.
     */
    init {
        // Create the neurons.
        for (i in 0 until numOfInput) {
            neuronInfoMap[i] = NeuronInfo(i, 0.0, NeuronType.Input)
        }

        for (o in 0 until numOfOutput) {
            neuronInfoMap[numOfInput + o] = NeuronInfo(numOfInput + o, 1.0, NeuronType.Output)
        }

        // Create the link innovations.
        for (i in 0 until numOfInput) {
            for (o in 0 until numOfOutput) {
                queryOrCreateInnovation(i, numOfInput + o).depth = 1
            }
        }

        nextNeuronID = numOfInput + numOfOutput

        if (bias) {
            val biasID = nextNeuronID++
            neuronInfoMap[biasID] = NeuronInfo(biasID, 0.0, NeuronType.Bias)

            for (i in 0 until numOfOutput) {
                queryOrCreateInnovation(biasID, numOfInput + i).depth = 1
            }
        }
    }

    /**
     * Returns the innovation between the neurons with IDs _fromNeuronID_ and _toNeuronID_. Creates a new innovation if not existing.
     *
     * @param fromNeuronID The starting neuron ID of the link.
     * @param toNeuronID The ending neuron ID of the link.
     * @return The retrieved or created innovation.
     */
    fun queryOrCreateInnovation(fromNeuronID: Int, toNeuronID: Int): Innovation {
        val existingInnovation =
            innovations.firstOrNull { x -> x.fromNeuronID == fromNeuronID && x.toNeuronID == toNeuronID }

        if (existingInnovation != null) {
            return existingInnovation
        }

        val newInnovation = Innovation(nextInnovationID++, fromNeuronID, toNeuronID)

        innovations.add(newInnovation)
        innovationMap[newInnovation.innovationID] = newInnovation

        if (!neuronInfoMap.containsKey(fromNeuronID)) neuronInfoMap[fromNeuronID] =
            NeuronInfo(fromNeuronID, -1.0, NeuronType.Hidden)
        else if (!neuronInfoMap.containsKey(toNeuronID)) neuronInfoMap[toNeuronID] =
            NeuronInfo(toNeuronID, -1.0, NeuronType.Hidden)

        return newInnovation
    }

    /**
     * Returns the type of the neuron with the given ID.
     *
     * @param neuronID The neuron ID.
     * @return The type of the neuron.
     */
    fun getNeuronType(neuronID: Int): NeuronType {
        return neuronInfoMap[neuronID]!!.type
    }

    /**
     * Returns the innovation with the given ID.
     *
     * @param id The innovation ID.
     * @return The innovation.
     */
    fun getInnovation(id: Int): Innovation {
        return innovationMap[id]!!
    }

    override fun toXElement(): XElement {
        val xIM = XElement("InnovationManager")

        val xInnovations = xIM.addChild("Innovations")
        innovations.forEach { xInnovations.add(it.toXElement()) }

        val xNeurons = xIM.addChild("Neurons")
        neuronInfoMap.forEach { _, u -> xNeurons.add(u.toXElement()) }

        return xIM
    }
}