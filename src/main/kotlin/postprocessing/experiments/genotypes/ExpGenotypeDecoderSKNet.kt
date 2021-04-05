package postprocessing.experiments.genotypes

import de.stevenkuenzel.xml.XElement
import elements.genotype.neuralnetworks.NetworkGenotype
import elements.genotype.newannencoding.SKNet
import elements.innovation.InnovationManager

/**
 * Deserializes neural network genomes from XElements.
 *
 * @constructor Creates a new instance.
 */
class ExpGenotypeDecoderSKNet : ExpGenotypeDecoder<SKNet>() {
    override fun loadGenome(xElement: XElement): SKNet {
        return SKNet.fromXElement(xElement)
    }

    override fun loadExperimentIteration(xIteration: XElement): ExpGenotypeDecoder<SKNet> {
        return ExpGenotypeDecoderSKNet()
    }
}