package elements.phenotype.realvectors

import de.stevenkuenzel.xml.XElement
import elements.genotype.realvectors.RealVectorGenotype
import elements.phenotype.Phenotype

/**
 * EXPERIMENTAL. Not part of my thesis.
 *
 * @constructor Creates a new instance of RealVectorPhenotype
 *
 * @param id The ID of the phenotype.
 * @param genotype The Genotype instance that created this Phenotype instance.
 */
class RealVectorPhenotype(id: Int, genotype: RealVectorGenotype) : Phenotype(id,
    genotype
) {
    override fun toXElement(): XElement {
        TODO("Not yet implemented")
    }

    override fun copy(): RealVectorPhenotype {
        return RealVectorPhenotype(id, genotype as RealVectorGenotype)
    }
}