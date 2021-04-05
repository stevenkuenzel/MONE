package elements.innovation

import de.stevenkuenzel.xml.XElement
import de.stevenkuenzel.xml.XLoadable
import de.stevenkuenzel.xml.XSavable
import elements.IDElement

/**
 * Contains meta information about a neuron.
 *
 * @property id The ID of the neuron.
 * @property splitX The x-position of the neuron, if it was printed on a chart from left to right. (Allows to determine whether a link is recurrent or not.)
 * @property type The type of the neuron.
 * @constructor Creates a new instance of NeuronInfo.
 */
class NeuronInfo(id : Int, var splitX : Double, var type : NeuronType) : IDElement(id), XSavable {
    companion object : XLoadable<NeuronInfo>    {
        /**
         * Only for loading a NeuronInfo from disk.
         */
        override fun fromXElement(xElement: XElement, vararg optional: Any): NeuronInfo {

            val id = xElement.getAttributeValueAsInt("ID")
            val splitX = xElement.getAttributeValueAsDouble("X")
            val type = NeuronType.valueOf(xElement.getAttributeValueAsString("Type"))

            return NeuronInfo(id, splitX, type)
        }

    }

    override fun toXElement(): XElement {
        val xInnovation = XElement("NeuronInfo")
        xInnovation.addAttribute("ID", id.toString())
        xInnovation.addAttribute("Type", type.name)
        xInnovation.addAttribute("X", splitX.toString())

        return xInnovation
    }
}