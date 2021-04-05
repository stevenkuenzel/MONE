package postprocessing.experiments.genotypes

import de.stevenkuenzel.xml.XElement
import elements.genotype.Genotype

/**
 * Deserializes XElements into a genotype of the specified type.
 *
 * @param T The genotype class.
 */
abstract class ExpGenotypeDecoder<T : Genotype<T>> {

    /**
     * Creates a new instance of the genotype decoder for the provided experiment iteration.
     *
     * @param xIteration The experiment iteration.
     * @return The genotype decoder.
     */
    abstract fun loadExperimentIteration(xIteration : XElement) : ExpGenotypeDecoder<T>

    /**
     * Deserializes a genome of the given type from an XElement.
     *
     * @param xElement The XElement.
     * @return The genotype instance.
     */
    abstract fun loadGenome(xElement: XElement) : T
}