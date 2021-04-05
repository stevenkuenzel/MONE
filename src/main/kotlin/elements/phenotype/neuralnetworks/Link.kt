package elements.phenotype.neuralnetworks

import de.stevenkuenzel.xml.XAttribute
import de.stevenkuenzel.xml.XElement
import de.stevenkuenzel.xml.XSavable

/**
 * A link between two neurons.
 *
 * @property weight The weight of the link.
 * @property from The starting neuron instance.
 * @property to The ending neuron instance.
 * @constructor Creates a new instance of Link.
 */
class Link(val weight: Double, val from: Neuron, val to: Neuron) : XSavable {
    /**
     * True, if the link is recurrent.
     */
    var timeDelay = false

    override fun toXElement(): XElement {
        return XElement(
            "Link",
            XAttribute("From", from.toString()),
            XAttribute("To", to.toString()),
            XAttribute("Weight", weight.toString())
        )
    }
}