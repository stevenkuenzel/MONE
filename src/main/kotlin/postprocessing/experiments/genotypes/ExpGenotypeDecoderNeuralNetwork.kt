package postprocessing.experiments.genotypes

import de.stevenkuenzel.xml.XElement
import elements.genotype.neuralnetworks.NetworkGenotype
import elements.innovation.InnovationManager

/**
 * Deserializes neural network genomes from XElements.
 *
 * @constructor Creates a new instance.
 */
class ExpGenotypeDecoderNeuralNetwork : ExpGenotypeDecoder<NetworkGenotype>() {
    var innovationManager: InnovationManager? = null

    override fun loadGenome(xElement: XElement): NetworkGenotype {
        return NetworkGenotype.fromXElement(xElement, innovationManager!!)
    }

    override fun loadExperimentIteration(xIteration: XElement): ExpGenotypeDecoder<NetworkGenotype> {
        return ExpGenotypeDecoderNeuralNetwork().also {
            it.innovationManager = InnovationManager.fromXElement(xIteration.getChild("InnovationManager")!!)
        }
    }
}