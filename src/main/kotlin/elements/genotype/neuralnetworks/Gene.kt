package elements.genotype.neuralnetworks

import org.apache.commons.math3.util.Precision
import de.stevenkuenzel.xml.XElement
import de.stevenkuenzel.xml.XLoadable
import de.stevenkuenzel.xml.XSavable

/**
 * A link gene.
 *
 * @property innovationID Innovation ID of the link.
 * @property weight Weight of the link.
 * @property enabled Is the link enabled?
 * @constructor Creates a new instance of Gene.
 */
data class Gene(var innovationID: Int, var weight: Double, var enabled: Boolean = true) : XSavable {

    companion object : XLoadable<Gene>
    {
        /**
         * Only for loading and evaluating experiment results.
         */
        override fun fromXElement(xElement: XElement, vararg optional: Any): Gene {

            val innovationID = xElement.getAttributeValueAsInt("Innovation")
            val weight = xElement.getAttributeValueAsDouble("Weight")

            return Gene(innovationID, weight, true)
        }

    }

    override fun toXElement(): XElement {
        val xGene = XElement("Gene")
        xGene.addAttribute("Innovation", innovationID.toString())
        xGene.addAttribute("Weight", Precision.round(weight, 4).toString())

        return xGene
    }

    override fun equals(other: Any?): Boolean {
        if (other is Gene) {
            return innovationID == other.innovationID
        }

        return super.equals(other)
    }

    override fun hashCode(): Int {
        return innovationID
    }

    override fun toString(): String {
        return "#$innovationID, w: $weight, e: $enabled"
    }
}