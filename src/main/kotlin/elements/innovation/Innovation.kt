package elements.innovation

import de.stevenkuenzel.xml.XElement
import de.stevenkuenzel.xml.XLoadable
import de.stevenkuenzel.xml.XSavable

/**
 * An innovation, based on Stanley's work.
 *
 * Source: Stanley, Kenneth Owen. Efficient evolution of neural networks through complexification. Diss. 2004.
 *
 * @property innovationID The innovation ID.
 * @property fromNeuronID The starting neuron of the link. If the innovation is a neuron-innovation, it represents the starting neuron of the link that is split by the neuron-innovation.
 * @property toNeuronID The ending neuron of the link. If the innovation is a neuron-innovation, it represents the ending neuron of the link that is split by the neuron-innovation.
 * @property splitNeuronID If this is a link-innovation, it contains the ID of the neuron that was placed on this link -- if existing.
 * @constructor Creates a new instance of Innovation.
 */
class Innovation(var innovationID: Int, var fromNeuronID: Int, var toNeuronID: Int, var splitNeuronID: Int = -1) :
    XSavable {
    companion object : XLoadable<Innovation> {
        /**
         * Only for loading an innovation from disk.
         */
        override fun fromXElement(xElement: XElement, vararg optional: Any): Innovation {
            val innovationID = xElement.getAttributeValueAsInt("ID")
            val fromNeuronID = xElement.getAttributeValueAsInt("NeuronFrom")
            val toNeuronID = xElement.getAttributeValueAsInt("NeuronTo")
            val splitNeuronID = xElement.getAttributeValueAsInt("NeuronSplitting")

            return Innovation(innovationID, fromNeuronID, toNeuronID, splitNeuronID)
        }
    }


    /**
     * Depth of the innovation within a neural network. Necessary for certain types of network-update-procedures.
     */
    var depth = -1

    override fun toXElement(): XElement {
        val xInnovation = XElement("Innovation")
        xInnovation.addAttribute("ID", innovationID.toString())
        xInnovation.addAttribute("NeuronFrom", fromNeuronID.toString())
        xInnovation.addAttribute("NeuronTo", toNeuronID.toString())
        xInnovation.addAttribute("NeuronSplitting", splitNeuronID.toString())

        return xInnovation
    }
}