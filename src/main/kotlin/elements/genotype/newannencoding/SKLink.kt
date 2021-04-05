package elements.genotype.newannencoding

import de.stevenkuenzel.xml.XAttribute
import de.stevenkuenzel.xml.XElement
import de.stevenkuenzel.xml.XLoadable
import de.stevenkuenzel.xml.XSavable

data class SKLink(val from : Int, val to : Int, var weight : Double = 1.0) : Comparable<SKLink>, XSavable{

    companion object : XLoadable<SKLink>
    {
        /**
         * Only for loading and evaluating experiment results.
         */
        override fun fromXElement(xElement: XElement, vararg optional: Any): SKLink {

            val id = xElement.getAttributeValueAsInt("ID")
            val weight = xElement.getAttributeValueAsDouble("Weight")

            val cInv = SKNet.cantorInv(id)

            return SKLink(cInv.first, cInv.second, weight)
        }

    }

    val linkID = SKNet.cantor(from, to)
    override fun toXElement(): XElement {
        return XElement("Gene", XAttribute("ID", linkID), XAttribute("Weight", weight))
    }

    override fun equals(other: Any?): Boolean {
        if (other is SKLink)
        {
            return linkID == other.linkID
        }

        return false
    }

    override fun compareTo(other: SKLink): Int {
        return linkID.compareTo(other.linkID)
    }
}