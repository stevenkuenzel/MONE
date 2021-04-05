package postprocessing.experiments.genotypes

import de.stevenkuenzel.xml.XElement
import elements.genotype.realvectors.RealVectorGenotype
import util.random.RandomProvider

/**
 * Deserializes real vector genomes from XElements.
 *
 * @constructor Creates a new instance.
 */
class ExpGenotypeDecoderRealVector : ExpGenotypeDecoder<RealVectorGenotype>() {
    override fun loadGenome(xElement: XElement): RealVectorGenotype {
        return RealVectorGenotype.fromXElement(xElement, RandomProvider.create())
    }

    override fun loadExperimentIteration(xIteration: XElement): ExpGenotypeDecoder<RealVectorGenotype> {
        return ExpGenotypeDecoderRealVector()
    }
}