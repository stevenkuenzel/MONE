package elements.phenotype.neuralnetworks

import elements.IDElement
import elements.innovation.NeuronType
import de.stevenkuenzel.xml.XAttribute
import de.stevenkuenzel.xml.XElement
import de.stevenkuenzel.xml.XSavable

/**
 * A neuron. Implementation based on Stanley's work.
 *
 * Source: Stanley, Kenneth Owen. Efficient evolution of neural networks through complexification. Diss. 2004.
 *
 * @property type The type of the neuron.
 * @constructor Creates a new instance of Neuron.
 *
 * @param id The ID of the neuron.
 */
class Neuron(id: Int, val type : NeuronType) : IDElement(id), XSavable {
    /**
     * Summed input from all incoming links.
     */
    var activeSum = 0.0

    /**
     * Output of the neuron (time t). (Value of _activeSum_ processed by the activation function).
     */
    var activation = 0.0

    /**
     * Output of the neuron in the previous time-step (t - 1).
     */
    var lastActivation = 0.0

    /**
     * Output of the neuron at time t - 2.
     */
    var lastActivation2 = 0.0

    /**
     * Number of times the activation function has been called.
     */
    var activationCount = 0

    /**
     * True, if the neuron was activated by another (incoming connection-) neuron or an input neuron in the current update procedure.
     */
    var activeFlag = false

    /**
     * List of incoming links. (Contributing to the activation.)
     */
    val incoming = mutableListOf<Link>()

    /**
     * List of outgoing links. (Activation is contributing to.)
     */
    val outgoing = mutableListOf<Link>()

    /**
     * Returns the activation of the neuron, if it has been activated at least once. Otherwise, returns 0.
     *
     * @return The activation of the neuron.
     */
    fun getActiveOut() : Double
    {
        return if (activationCount > 0) activation else 0.0
    }

    /**
     * Returns the activation of the neuron in the previous time-step, if it has been activated at least twice. Otherwise, returns 0.
     *
     * @return The activation of the neuron in the previous time-step.
     */
    fun getActiveOutTimeDelay() : Double
    {
        return if (activationCount > 1) lastActivation else 0.0
    }

    /**
     * Sets the input value of the neuron.
     *
     * @param value The input value.
     */
    fun setInputValue(value : Double)
    {
        if (type == NeuronType.Input)
        {
            lastActivation2 = lastActivation
            lastActivation = activation

            activation = value
            activationCount++
        }
    }

    fun copy() : Neuron
    {
        return Neuron(id, type)
    }

    override fun toXElement(): XElement {
        return XElement("Neuron", XAttribute("ID", id), XAttribute("Type", type.name))
    }
}